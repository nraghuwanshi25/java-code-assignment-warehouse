package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.FulfilmentEntity;
import com.fulfilment.application.monolith.warehouses.adapters.database.FulfilmentRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class FulfilmentUseCaseFullCoverageTest {

  @Inject
  FulfilmentUseCase fulfilmentUseCase;

  @Inject
  FulfilmentRepository fulfilmentRepository;

  @Inject
  WarehouseRepository warehouseRepository;

  @Inject
  ProductRepository productRepository;

  @BeforeEach
  @Transactional
  public void cleanup() {
    fulfilmentRepository.deleteAll();
  }

  @Test
  @Transactional
  public void successAndDuplicateWarehouseEntriesDoNotCountTwice() {
    Product p = new Product(); p.name = "P-FC-1"; p.stock = 1; productRepository.persist(p);
    Store s = new Store("S-FC-1"); s.persist();
    Warehouse w = new Warehouse(); w.businessUnitCode = "W-FC-1"; w.location = "L"; w.capacity = 10; w.stock = 0; warehouseRepository.create(w);

    // add duplicate fulfilment entries pointing to same warehouse (should count as 1 distinct)
    fulfilmentRepository.persist(new FulfilmentEntity(p.id, s.id, w.businessUnitCode));
    fulfilmentRepository.persist(new FulfilmentEntity(p.id, s.id, w.businessUnitCode));

    // Now associate same product to same store but another warehouse should be allowed once (distinct becomes 1 -> adding second distinct allowed until 2)
    Warehouse w2 = new Warehouse(); w2.businessUnitCode = "W-FC-2"; w2.location = "L"; w2.capacity = 10; w2.stock = 0; warehouseRepository.create(w2);

    fulfilmentUseCase.associate(p.id, s.id, w2.businessUnitCode);

    List<FulfilmentEntity> list = fulfilmentRepository.findByProductAndStore(p.id, s.id);
    // should contain at least two entries (maybe duplicates)
    assertEquals(3, list.size());
  }

  @Test
  @Transactional
  public void enforceProductPerStoreLimitExactly() {
    Product p = new Product(); p.name = "P-FC-2"; p.stock = 1; productRepository.persist(p);
    Store s = new Store("S-FC-2"); s.persist();
    Warehouse wa = createWarehouse("WA");
    Warehouse wb = createWarehouse("WB");
    Warehouse wc = createWarehouse("WC");

    // associate two distinct warehouses
    fulfilmentRepository.persist(new FulfilmentEntity(p.id, s.id, wa.businessUnitCode));
    fulfilmentRepository.persist(new FulfilmentEntity(p.id, s.id, wb.businessUnitCode));

    // third distinct should be rejected
    assertThrows(WebApplicationException.class, () -> fulfilmentUseCase.associate(p.id, s.id, wc.businessUnitCode));
  }

  @Test
  @Transactional
  public void enforceStoreLimitExactly() {
    Product p1 = createProduct("PP1");
    Product p2 = createProduct("PP2");
    Product p3 = createProduct("PP3");
    Product p4 = createProduct("PP4");

    Store s = new Store("S-FC-3"); s.persist();

    Warehouse wa = createWarehouse("W1-FC");
    Warehouse wb = createWarehouse("W2-FC");
    Warehouse wc = createWarehouse("W3-FC");
    Warehouse wd = createWarehouse("W4-FC");

    fulfilmentRepository.persist(new FulfilmentEntity(p1.id, s.id, wa.businessUnitCode));
    fulfilmentRepository.persist(new FulfilmentEntity(p2.id, s.id, wb.businessUnitCode));
    fulfilmentRepository.persist(new FulfilmentEntity(p3.id, s.id, wc.businessUnitCode));

    assertThrows(WebApplicationException.class, () -> fulfilmentUseCase.associate(p4.id, s.id, wd.businessUnitCode));
  }

  @Test
  @Transactional
  public void enforceWarehouseProductLimitExactly() {
    Store s = new Store("S-FC-4"); s.persist();
    Warehouse w = createWarehouse("W-FC-LIMIT");

    for (int i = 0; i < 5; i++) {
      Product p = createProduct("WPFC" + i);
      fulfilmentRepository.persist(new FulfilmentEntity(p.id, s.id, w.businessUnitCode));
    }

    Product extra = createProduct("WPX-FC");
    assertThrows(WebApplicationException.class, () -> fulfilmentUseCase.associate(extra.id, s.id, w.businessUnitCode));
  }

  private Product createProduct(String name) {
    Product p = new Product(); p.name = name; p.stock = 1; productRepository.persist(p); return p;
  }

  private Warehouse createWarehouse(String buc) {
    Warehouse w = new Warehouse(); w.businessUnitCode = buc; w.location = "L"; w.capacity = 100; w.stock = 0; warehouseRepository.create(w); return w;
  }

  @Test
  @Transactional
  public void productNotFoundThrows404() {
    // create store and warehouse but not product
    Store s = new Store("S-MISS-P");
    s.persist();

    Warehouse w = new Warehouse();
    w.businessUnitCode = "W-MISS-P";
    w.location = "LOC";
    w.capacity = 10;
    w.stock = 0;
    warehouseRepository.create(w);

    assertThrows(WebApplicationException.class, () -> fulfilmentUseCase.associate(99999L, s.id, w.businessUnitCode));
  }

  @Test
  @Transactional
  public void storeNotFoundThrows404() {
    // create product and warehouse but not store
    Product p = new Product();
    p.name = "P-MISS-S";
    p.stock = 1;
    productRepository.persist(p);

    Warehouse w = new Warehouse();
    w.businessUnitCode = "W-MISS-S";
    w.location = "LOC";
    w.capacity = 10;
    w.stock = 0;
    warehouseRepository.create(w);

    assertThrows(WebApplicationException.class, () -> fulfilmentUseCase.associate(p.id, 99999L, w.businessUnitCode));
  }

  @Test
  @Transactional
  public void warehouseNotFoundThrows404() {
    // create product and store but not warehouse
    Product p = new Product();
    p.name = "P-MISS-W";
    p.stock = 1;
    productRepository.persist(p);

    Store s = new Store("S-MISS-W");
    s.persist();

    assertThrows(WebApplicationException.class, () -> fulfilmentUseCase.associate(p.id, s.id, "NO-WH"));
  }
  @Test
  @Transactional
  public void createFindUpdateRemoveFlow() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "IT-BUC-1";
    w.location = "LOC-IT";
    w.capacity = 10;
    w.stock = 0;

    warehouseRepository.create(w);

    // find by bu code returns domain
    Warehouse found = warehouseRepository.findByBusinessUnitCode("IT-BUC-1");
    assertNotNull(found);
    assertEquals("LOC-IT", found.location);

    // find db by bu code
    DbWarehouse db = warehouseRepository.findDbByBusinessUnitCode("IT-BUC-1");
    assertNotNull(db);
    assertEquals("IT-BUC-1", db.businessUnitCode);

    // find by id
    DbWarehouse byId = warehouseRepository.findDbById(db.id);
    assertNotNull(byId);

    // update existing
    Warehouse upd = new Warehouse();
    upd.businessUnitCode = "IT-BUC-1";
    upd.location = "LOC-UPDATED";
    upd.capacity = 20;
    upd.stock = 1;
    warehouseRepository.update(upd);

    Warehouse after = warehouseRepository.findByBusinessUnitCode("IT-BUC-1");
    assertEquals("LOC-UPDATED", after.location);
    assertEquals(Integer.valueOf(20), after.capacity);

    // remove
    warehouseRepository.remove(upd);
    assertNull(warehouseRepository.findDbByBusinessUnitCode("IT-BUC-1"));
  }

  @Test
  @Transactional
  public void getAllMapsDbToDomainAndNoOpUpdateRemove() {
    // create two entries directly
    Warehouse a = new Warehouse(); a.businessUnitCode = "IT-A"; a.location = "LA"; a.capacity = 5; a.stock = 1; warehouseRepository.create(a);
    Warehouse b = new Warehouse(); b.businessUnitCode = "IT-B"; b.location = "LB"; b.capacity = 6; b.stock = 2; warehouseRepository.create(b);

    List<Warehouse> list = warehouseRepository.getAll();
    assertTrue(list.size() >= 2);

    // update non-existing should be no-op
    Warehouse missing = new Warehouse(); missing.businessUnitCode = "NO-EXIST"; missing.location = "X"; missing.capacity = 1; missing.stock = 0;
    warehouseRepository.update(missing); // no exception

    // remove non-existing should be no-op
    warehouseRepository.remove(missing); // no exception
  }
}

