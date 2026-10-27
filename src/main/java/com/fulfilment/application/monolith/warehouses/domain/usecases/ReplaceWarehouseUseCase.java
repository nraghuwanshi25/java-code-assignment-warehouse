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

    // find existing by businessUnitCode
    var existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new WebApplicationException("Warehouse unit not found", 404);
    }

    Location loc = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (loc == null) {
      throw new WebApplicationException("Invalid request parameters", 400);
    }

    if (newWarehouse.capacity < existing.getStock()) {
      throw new WebApplicationException("new capacity cannot accommodate existing stock", 400);
    }
    if (!newWarehouse.stock.equals(existing.getStock())) {
      throw new WebApplicationException("replacement must have same stock as existing", 400);
    }

    // Warehouse-count constraint on target location (exclude the outgoing warehouse)
    long countAtTarget = warehouseStore.getAll().stream()
        .filter(w -> w.getLocation() != null && w.getLocation().equals(newWarehouse.location))
        .count();
    if (existing.getLocation() != null && existing.getLocation().equals(newWarehouse.location)) {
      // replacing in same location -> exclude the existing one
      countAtTarget = Math.max(0, countAtTarget - 1);
    }
    if (countAtTarget >= loc.maxNumberOfWarehouses) {
      throw new WebApplicationException("maximum number of warehouses reached for location", 400);
    }

    // location capacity constraints: sum capacities of active warehouses at the target location
    int currentSumCapacity = warehouseStore.getAll().stream()
        .filter(w -> w.getLocation() != null && w.getLocation().equals(newWarehouse.location))
        .mapToInt(w -> w.getCapacity() == null ? 0 : w.getCapacity())
        .sum();
    int outgoingCapacity = existing.getCapacity() == null ? 0 : existing.getCapacity();
    int effectiveCurrent = currentSumCapacity - (existing.getLocation() != null && existing.getLocation().equals(newWarehouse.location) ? outgoingCapacity : 0);

    if ((long) effectiveCurrent + newWarehouse.capacity > loc.maxCapacity) {
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
