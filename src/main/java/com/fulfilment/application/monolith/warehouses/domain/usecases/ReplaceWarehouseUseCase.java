package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;

  @Inject
  LocationResolver locationResolver;

  @Inject
  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse == null || newWarehouse.businessUnitCode == null) {
      throw new WebApplicationException("businessUnitCode is required for replacement", 400);
    }

    // find existing by businessUnitCode
    var existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new WebApplicationException("warehouse not found", 404);
    }

    Location loc = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (loc == null) {
      throw new WebApplicationException("invalid location", 400);
    }

    if (newWarehouse.capacity == null || newWarehouse.capacity < existing.getStock()) {
      throw new WebApplicationException("new capacity cannot accommodate existing stock", 400);
    }
    if (!newWarehouse.stock.equals(existing.getStock())) {
      throw new WebApplicationException("replacement must have same stock as existing", 400);
    }

    // location capacity constraints
    if (newWarehouse.capacity > loc.maxCapacity) {
      throw new WebApplicationException("capacity exceeds location maximum", 400);
    }
    if (newWarehouse.stock > newWarehouse.capacity) {
      throw new WebApplicationException("stock exceeds capacity", 400);
    }

    // archive existing and create new using the same business unit code
    existing.setArchivedAt(LocalDateTime.now());
    warehouseStore.update(existing);

    newWarehouse.businessUnitCode = existing.getBusinessUnitCode();
    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
  }
}
