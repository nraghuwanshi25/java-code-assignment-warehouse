package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    // return only active (not archived) warehouses
    return this.list("archivedAt is null").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    var db = new DbWarehouse();
    db.businessUnitCode = warehouse.businessUnitCode;
    db.location = warehouse.location;
    db.capacity = warehouse.capacity;
    db.stock = warehouse.stock;
    db.createdAt = LocalDateTime.now();
    db.archivedAt = warehouse.archivedAt;
    persist(db);
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    // find the active DB record by businessUnitCode and update fields
    // (avoid matching archived records when updating the active warehouse)
    var db = find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode).firstResult();
    if (db != null) {
      db.location = warehouse.location;
      db.capacity = warehouse.capacity;
      db.stock = warehouse.stock;
      db.archivedAt = warehouse.archivedAt;
      // Panache will persist changes of managed entity
      persist(db);
    }
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    var db = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (db != null) {
      delete(db);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    // only return active (not archived) warehouse for normal lookups
    var db = find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
    return db == null ? null : db.toWarehouse();
  }

  // returns a Warehouse regardless of archived state (used by archive operation to detect already archived)
  public Warehouse findAnyByBusinessUnitCodeAll(String buCode) {
    var db = find("businessUnitCode = ?1", buCode).firstResult();
    return db == null ? null : db.toWarehouse();
  }

  public DbWarehouse findDbByBusinessUnitCode(String buCode) {
    return find("businessUnitCode", buCode).firstResult();
  }

  // helper to find DbWarehouse by numeric id
  public DbWarehouse findDbById(Long id) {
    return findById(id);
  }

}
