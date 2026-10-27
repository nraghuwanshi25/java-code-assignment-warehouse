package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class WarehouseResourceImplTest {

  private WarehouseResourceImpl resource;
  private TestRepo repo;
  private TestCreateUseCase createUC;
  private TestReplaceUseCase replaceUC;
  private TestArchiveUseCase archiveUC;

  @BeforeEach
  void setUp() throws Exception {
    resource = new WarehouseResourceImpl();
    repo = new TestRepo();
    createUC = new TestCreateUseCase();
    replaceUC = new TestReplaceUseCase();
    archiveUC = new TestArchiveUseCase();

    setField(resource, "warehouseRepository", repo);
    setField(resource, "createWarehouseUseCase", createUC);
    setField(resource, "replaceWarehouseUseCase", replaceUC);
    setField(resource, "archiveWarehouseUseCase", archiveUC);
  }

  private void setField(Object target, String name, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Test
  void listAllWarehousesUnits_returnsMappedList() {
    repo.addDomain(1L, "MWH.001", "L1", 100, 10);
    repo.addDomain(2L, "MWH.012", "L2", 50, 5);

    Response r = resource.listAllWarehousesUnits();
    assertEquals(200, r.getStatus());
    @SuppressWarnings("unchecked")
    List<com.warehouse.api.beans.Warehouse> list = (List<com.warehouse.api.beans.Warehouse>) r.getEntity();
    assertEquals(2, list.size());
    assertEquals("MWH.001", list.get(0).getBusinessUnitCode());
  }

  @Test
  void createANewWarehouseUnit_delegates_and_returns201() {
    com.warehouse.api.beans.Warehouse api = new com.warehouse.api.beans.Warehouse();
    api.setBusinessUnitCode("NEW-BUC");
    api.setLocation("L");
    api.setCapacity(10);
    api.setStock(0);

    repo.addDomain(3L, "NEW-BUC", "L", 10, 0);

    Response r = resource.createANewWarehouseUnit(api);
    assertEquals(201, r.getStatus());
    com.warehouse.api.beans.Warehouse resp = (com.warehouse.api.beans.Warehouse) r.getEntity();
    assertEquals("NEW-BUC", resp.getBusinessUnitCode());
    assertTrue(createUC.invoked);
  }

  @Test
  void getAWarehouseUnitByID_notFound_returns404() {
    Response r = resource.getAWarehouseUnitByID("999");
    assertEquals(404, r.getStatus());
  }

  // Helpers
  static class TestRepo extends WarehouseRepository {
    List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> list = new ArrayList<>();

    void addDomain(Long id, String buc, String loc, Integer cap, Integer stock) {
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse d = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
      d.businessUnitCode = buc;
      d.location = loc;
      d.capacity = cap;
      d.stock = stock;
      list.add(d);
    }

    @Override
    public List<com.fulfilment.application.monolith.warehouses.domain.models.Warehouse> getAll() {
      return new ArrayList<>(list);
    }

    @Override
    public com.fulfilment.application.monolith.warehouses.domain.models.Warehouse findByBusinessUnitCode(String buCode) {
      return list.stream().filter(w -> w.businessUnitCode.equals(buCode)).findFirst().orElse(null);
    }

    @Override
    public DbWarehouse findDbById(Long id) {
      if (id <= list.size()) {
        DbWarehouse db = new DbWarehouse();
        var w = list.get(id.intValue()-1);
        db.id = id;
        db.businessUnitCode = w.businessUnitCode;
        db.location = w.location;
        db.capacity = w.capacity;
        db.stock = w.stock;
        return db;
      }
      return null;
    }
  }

  static class TestCreateUseCase extends CreateWarehouseUseCase {
    boolean invoked = false;
    TestCreateUseCase() { super(null); }
    @Override
    public void create(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
      invoked = true;
    }
  }

  static class TestReplaceUseCase extends ReplaceWarehouseUseCase {
    boolean invoked = false;
    TestReplaceUseCase() { super(null); }
    @Override
    public void replace(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
      invoked = true;
    }
  }

  static class TestArchiveUseCase extends ArchiveWarehouseUseCase {
    boolean invoked = false;
    TestArchiveUseCase() { super(null); }
    @Override
    public void archive(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
      invoked = true;
    }
  }
}

