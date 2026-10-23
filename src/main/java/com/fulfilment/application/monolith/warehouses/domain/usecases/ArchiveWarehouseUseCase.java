package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  @Inject
  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    if (warehouse == null || warehouse.getBusinessUnitCode() == null) {
      throw new WebApplicationException("invalid warehouse", 400);
    }

    var existing = warehouseStore.findByBusinessUnitCode(warehouse.getBusinessUnitCode());
    if (existing == null) {
      throw new WebApplicationException("warehouse not found", 404);
    }

    existing.setArchivedAt(LocalDateTime.now());
    warehouseStore.update(existing);
  }
}
