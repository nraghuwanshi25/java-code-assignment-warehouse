package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private ArchiveWarehouseUseCase useCase;
  private InMemoryWarehouseStore store;

  @BeforeEach
  public void setup() {
    store = new InMemoryWarehouseStore();
    useCase = new ArchiveWarehouseUseCase(store);
  }

  @Test
  public void archiveExisting() {
    var existing = new Warehouse();
    existing.businessUnitCode = "BUC-X";
    existing.location = "L1";
    existing.capacity = 50;
    existing.stock = 5;
    store.create(existing);

    useCase.archive(existing);
    assertEquals(1, store.updated.size());
    assertNotNull(store.updated.get(0).getArchivedAt());
  }

  @Test
  public void rejectInvalid() {
    assertThrows(NullPointerException.class, () -> useCase.archive(null));
  }

  // In-memory test doubles
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

}
