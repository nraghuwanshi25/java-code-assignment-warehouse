package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
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
  public void update(Warehouse warehouse) {
    // find by businessUnitCode and update fields
    var db = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (db != null) {
      db.location = warehouse.location;
      db.capacity = warehouse.capacity;
      db.stock = warehouse.stock;
      db.archivedAt = warehouse.archivedAt;
      // Panache will persist changes of managed entity
      persist(db);
    }
    // if not found, do nothing (no-op)
  }

  @Override
  public void remove(Warehouse warehouse) {
    var db = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (db != null) {
      delete(db);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    var db = find("businessUnitCode", buCode).firstResult();
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
