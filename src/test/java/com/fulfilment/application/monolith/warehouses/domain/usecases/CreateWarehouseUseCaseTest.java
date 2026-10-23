package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

    private CreateWarehouseUseCase useCase;
    private InMemoryWarehouseStore store;
    private InMemoryLocationResolver locationResolver;

    @BeforeEach
    public void setup() {
        store = new InMemoryWarehouseStore();
        locationResolver = new InMemoryLocationResolver();
        useCase = new CreateWarehouseUseCase(store);
        useCase.locationResolver = locationResolver; // package-visible injection imitation
    }

    @Test
    public void createValidWarehouse() {
        var w = new Warehouse();
        w.businessUnitCode = "BUC1";
        w.location = "LOC1";
        w.capacity = 100;
        w.stock = 10;

        locationResolver.addLocation("LOC1", 2, 200);

        useCase.create(w);

        assertEquals(1, store.created.size());
        assertEquals("BUC1", store.created.get(0).businessUnitCode);
        assertNotNull(store.created.get(0).createdAt);
    }

    @Test
    public void rejectDuplicateBUC() {
        var existing = new Warehouse();
        existing.businessUnitCode = "BUC2";
        store.create(existing);

        var w = new Warehouse();
        w.businessUnitCode = "BUC2";
        w.location = "LOC1";
        w.capacity = 50;
        w.stock = 5;

        locationResolver.addLocation("LOC1", 2, 100);

        assertThrows(WebApplicationException.class, () -> useCase.create(w));
    }

    @Test
    public void rejectInvalidLocation() {
        var w = new Warehouse();
        w.businessUnitCode = "BUC3";
        w.location = "UNKNOWN";
        w.capacity = 10;
        w.stock = 1;

        assertThrows(WebApplicationException.class, () -> useCase.create(w));
    }

    @Test
    public void rejectMaxWarehousesReached() {
        var loc = "LOC2";
        locationResolver.addLocation(loc, 1, 100);

        var existing = new Warehouse();
        existing.businessUnitCode = "BUC-A";
        existing.location = loc;
        store.create(existing);

        var w = new Warehouse();
        w.businessUnitCode = "BUC-B";
        w.location = loc;
        w.capacity = 10;
        w.stock = 1;

        assertThrows(WebApplicationException.class, () -> useCase.create(w));
    }

    @Test
    public void rejectInvalidCapacity() {
        var w = new Warehouse();
        w.businessUnitCode = "BUC-C";
        w.location = "LOC3";
        // capacity null
        w.capacity = null;
        w.stock = 1;

        locationResolver.addLocation("LOC3", 2, 100);

        assertThrows(WebApplicationException.class, () -> useCase.create(w));
    }

    @Test
    public void rejectInvalidStock() {
        var w = new Warehouse();
        w.businessUnitCode = "BUC-D";
        w.location = "LOC4";
        w.capacity = 10;
        w.stock = -1; // invalid negative

        locationResolver.addLocation("LOC4", 2, 100);

        assertThrows(WebApplicationException.class, () -> useCase.create(w));

        // also null stock
        var w2 = new Warehouse();
        w2.businessUnitCode = "BUC-D2";
        w2.location = "LOC4";
        w2.capacity = 10;
        w2.stock = null;

        assertThrows(WebApplicationException.class, () -> useCase.create(w2));
    }

    @Test
    public void rejectCapacityExceedsLocationMax() {
        var w = new Warehouse();
        w.businessUnitCode = "BUC-E";
        w.location = "LOC5";
        w.capacity = 500;
        w.stock = 1;

        locationResolver.addLocation("LOC5", 5, 100);

        assertThrows(WebApplicationException.class, () -> useCase.create(w));
    }

    @Test
    public void rejectStockExceedsCapacity() {
        var w = new Warehouse();
        w.businessUnitCode = "BUC-F";
        w.location = "LOC6";
        w.capacity = 5;
        w.stock = 10;

        locationResolver.addLocation("LOC6", 5, 100);

        assertThrows(WebApplicationException.class, () -> useCase.create(w));
    }

    // In-memory test doubles
    static class InMemoryWarehouseStore implements WarehouseStore {
        List<Warehouse> created = new ArrayList<>();

        @Override
        public List<Warehouse> getAll() {
            // return created items so tests that count existing warehouses by location work
            return new ArrayList<>(created);
        }

        @Override
        public void create(Warehouse warehouse) {
            created.add(warehouse);
        }

        @Override
        public void update(Warehouse warehouse) {
            // no-op
        }

        @Override
        public void remove(Warehouse warehouse) {
            // no-op
        }

        @Override
        public Warehouse findByBusinessUnitCode(String buCode) {
            return created.stream().filter(w -> buCode.equals(w.businessUnitCode)).findFirst().orElse(null);
        }
    }

    static class InMemoryLocationResolver implements LocationResolver {
        private final List<Location> locs = new ArrayList<>();

        void addLocation(String id, int maxNumberOfWarehouses, int maxCapacity) {
            locs.add(new Location(id, maxNumberOfWarehouses, maxCapacity));
        }

        @Override
        public Location resolveByIdentifier(String identifier) {
            return locs.stream().filter(l -> l.identification.equals(identifier)).findFirst().orElse(null);
        }
    }

    @Test
    public void testDomainWarehouseGettersAndSetters() {
        Warehouse w = new Warehouse();
        // defaults should be null
        assertNull(w.getBusinessUnitCode());
        assertNull(w.getLocation());
        assertNull(w.getCapacity());
        assertNull(w.getStock());
        assertNull(w.getCreatedAt());
        assertNull(w.getArchivedAt());

        w.setBusinessUnitCode("BUC-1");
        w.setLocation("LOC-1");
        w.setCapacity(42);
        w.setStock(7);
        var now = LocalDateTime.now();
        w.setCreatedAt(now);
        w.setArchivedAt(now);

        assertEquals("BUC-1", w.getBusinessUnitCode());
        assertEquals("LOC-1", w.getLocation());
        assertEquals(Integer.valueOf(42), w.getCapacity());
        assertEquals(Integer.valueOf(7), w.getStock());
        assertEquals(now, w.getCreatedAt());
        assertEquals(now, w.getArchivedAt());
    }
}
