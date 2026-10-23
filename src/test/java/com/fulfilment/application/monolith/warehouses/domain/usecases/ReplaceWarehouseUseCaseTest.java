package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

    private ReplaceWarehouseUseCase useCase;
    private InMemoryWarehouseStore store;
    private InMemoryLocationResolver locationResolver;

    @BeforeEach
    public void setup() {
        store = new InMemoryWarehouseStore();
        locationResolver = new InMemoryLocationResolver();
        useCase = new ReplaceWarehouseUseCase(store);
        useCase.locationResolver = locationResolver;
    }

    @Test
    public void replaceSuccessful() {
        var existing = new Warehouse();
        existing.businessUnitCode = "BUC-R";
        existing.location = "L1";
        existing.capacity = 100;
        existing.stock = 10;
        store.create(existing);

        locationResolver.addLocation("L1", 5, 200);

        var replacement = new Warehouse();
        replacement.businessUnitCode = "BUC-R";
        replacement.location = "L1";
        replacement.capacity = 150;
        replacement.stock = 10;

        useCase.replace(replacement);

        assertEquals(1, store.updated.size());
        assertEquals("BUC-R", store.created.get(0).businessUnitCode);
    }

    @Test
    public void rejectReplacementIfStockMismatch() {
        var existing = new Warehouse();
        existing.businessUnitCode = "BUC-S";
        existing.location = "L2";
        existing.capacity = 50;
        existing.stock = 5;
        store.create(existing);

        locationResolver.addLocation("L2", 5, 100);

        var replacement = new Warehouse();
        replacement.businessUnitCode = "BUC-S";
        replacement.location = "L2";
        replacement.capacity = 60;
        replacement.stock = 6; // mismatch

        assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
    }

    @Test
    public void rejectWhenNewWarehouseIsNullOrMissingBUC() {
        // null
        assertThrows(WebApplicationException.class, () -> useCase.replace(null));

        // missing businessUnitCode
        var replacement = new Warehouse();
        replacement.businessUnitCode = null;
        assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
    }

    @Test
    public void rejectWhenExistingNotFound() {
        var replacement = new Warehouse();
        replacement.businessUnitCode = "NO-EXIST";
        replacement.location = "L-NO";
        replacement.capacity = 10;
        replacement.stock = 1;

        // no existing in store -> should throw 404
        assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
    }

    @Test
    public void rejectInvalidLocation() {
        var existing = new Warehouse();
        existing.businessUnitCode = "BUC-IL";
        existing.location = "L-IL";
        existing.capacity = 100;
        existing.stock = 5;
        store.create(existing);

        var replacement = new Warehouse();
        replacement.businessUnitCode = "BUC-IL";
        replacement.location = "UNKNOWN-LOC"; // will not be resolved
        replacement.capacity = 100;
        replacement.stock = 5;

        assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
    }

    @Test
    public void rejectNewCapacityTooSmallOrNull() {
        var existing = new Warehouse();
        existing.businessUnitCode = "BUC-CS";
        existing.location = "L-CS";
        existing.capacity = 100;
        existing.stock = 10;
        store.create(existing);

        locationResolver.addLocation("L-CS", 5, 200);

        var replacementSmall = new Warehouse();
        replacementSmall.businessUnitCode = "BUC-CS";
        replacementSmall.location = "L-CS";
        replacementSmall.capacity = 5; // smaller than existing stock
        replacementSmall.stock = 10;

        assertThrows(WebApplicationException.class, () -> useCase.replace(replacementSmall));

        var replacementNullCap = new Warehouse();
        replacementNullCap.businessUnitCode = "BUC-CS";
        replacementNullCap.location = "L-CS";
        replacementNullCap.capacity = null;
        replacementNullCap.stock = 10;

        assertThrows(WebApplicationException.class, () -> useCase.replace(replacementNullCap));
    }

    @Test
    public void rejectCapacityExceedsLocationMax() {
        var existing = new Warehouse();
        existing.businessUnitCode = "BUC-CM";
        existing.location = "L-CM";
        existing.capacity = 50;
        existing.stock = 5;
        store.create(existing);

        locationResolver.addLocation("L-CM", 5, 100); // maxCapacity 100

        var replacement = new Warehouse();
        replacement.businessUnitCode = "BUC-CM";
        replacement.location = "L-CM";
        replacement.capacity = 200; // exceeds location max
        replacement.stock = 5;

        assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
    }

    @Test
    public void rejectStockExceedsCapacity() {
        var existing = new Warehouse();
        existing.businessUnitCode = "BUC-SC";
        existing.location = "L-SC";
        existing.capacity = 100;
        existing.stock = 10;
        store.create(existing);

        locationResolver.addLocation("L-SC", 5, 200);

        var replacement = new Warehouse();
        replacement.businessUnitCode = "BUC-SC";
        replacement.location = "L-SC";
        replacement.capacity = 5;
        replacement.stock = 10; // stock > capacity

        assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
    }

    // in-memory doubles
    static class InMemoryWarehouseStore implements WarehouseStore {
        List<Warehouse> created = new ArrayList<>();
        List<Warehouse> updated = new ArrayList<>();

        @Override
        public List<Warehouse> getAll() {
            return new ArrayList<>();
        }

        @Override
        public void create(Warehouse warehouse) {
            created.add(warehouse);
        }

        @Override
        public void update(Warehouse warehouse) {
            updated.add(warehouse);
        }

        @Override
        public void remove(Warehouse warehouse) {}

        @Override
        public Warehouse findByBusinessUnitCode(String buCode) {
            return created.stream().filter(w -> buCode.equals(w.businessUnitCode)).findFirst().orElse(null);
        }
    }

    static class InMemoryLocationResolver implements LocationResolver {
        private final List<Location> locs = new ArrayList<>();

        void addLocation(String id, int max, int cap) {
            locs.add(new Location(id, max, cap));
        }

        @Override
        public Location resolveByIdentifier(String identifier) {
            return locs.stream().filter(l -> l.identification.equals(identifier)).findFirst().orElse(null);
        }
    }
}
