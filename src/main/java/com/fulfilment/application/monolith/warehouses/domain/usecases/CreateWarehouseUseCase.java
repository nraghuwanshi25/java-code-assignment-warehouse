package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;

  @Inject
  LocationResolver locationResolver;

  @Inject
  public CreateWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException("businessUnitCode already exists", 409);
    }

    Location loc = locationResolver.resolveByIdentifier(warehouse.location);
    if (loc == null) {
      throw new WebApplicationException("Invalid request parameters", 400);
    }

    long existingAtLocation = warehouseStore.getAll().stream()
        .filter(w -> w.getLocation() != null && w.getLocation().equals(warehouse.location))
        .count();
    if (existingAtLocation >= loc.maxNumberOfWarehouses) {
      throw new WebApplicationException("maximum number of warehouses reached for location", 400);
    }

    // capacity and stock already validated above for non-null and non-negative

    // location capacity constraint: sum of capacities of active warehouses at this location
    int currentSumCapacity = warehouseStore.getAll().stream()
        .filter(w -> w.getLocation() != null && w.getLocation().equals(warehouse.location))
        .mapToInt(w -> w.getCapacity() == null ? 0 : w.getCapacity())
        .sum();
    if ((long) currentSumCapacity + warehouse.capacity > loc.maxCapacity) {
      throw new WebApplicationException("capacity exceeds location maximum", 400);
    }

    if (warehouse.capacity > loc.maxCapacity) {
      throw new WebApplicationException("capacity exceeds location maximum", 400);
    }
    if (warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException("stock exceeds capacity", 400);
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
  }
}
