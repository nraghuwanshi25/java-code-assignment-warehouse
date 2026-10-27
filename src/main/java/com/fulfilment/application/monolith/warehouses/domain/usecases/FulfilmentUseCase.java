package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.adapters.database.FulfilmentEntity;
import com.fulfilment.application.monolith.warehouses.adapters.database.FulfilmentRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class FulfilmentUseCase {

  @Inject FulfilmentRepository fulfilmentRepository;
  @Inject WarehouseRepository warehouseRepository;
  @Inject ProductRepository productRepository;

  public void associate(Long productId, Long storeId, String warehouseBuc) {
    // validations
    // 1) product existence
    var product = productRepository.findById(productId);
    if (product == null) {
      throw new WebApplicationException("product not found", 404);
    }

    // 2) store existence
    var store = Store.findById(storeId);
    if (store == null) {
      throw new WebApplicationException("store not found", 404);
    }

    // 3) warehouse existence (active)
    var warehouse = warehouseRepository.findByBusinessUnitCode(warehouseBuc);
    if (warehouse == null) {
      throw new WebApplicationException("warehouse not found", 404);
    }

    // fetch existing fulfilments
    var existingForProduct = fulfilmentRepository.findByProductAndStore(productId, storeId);

    // Reject exact duplicate association (same product, store and warehouse)
    boolean duplicate = existingForProduct.stream().anyMatch(e -> warehouseBuc.equals(e.warehouseBusinessUnitCode));
    if (duplicate) {
      throw new WebApplicationException("fulfilment association already exists", 409);
    }

    // Constraint 1: product per store max 2 warehouses (count distinct active warehouse codes for product+store)
    long distinctForProduct = existingForProduct.stream()
        .map(e -> e.warehouseBusinessUnitCode)
        .distinct()
        .filter(buc -> warehouseRepository.findByBusinessUnitCode(buc) != null)
        .count();
    // if the target warehouse isn't already counted, associating will increase distinct count by 1
    long distinctForProductAfter = distinctForProduct + (warehouseRepository.findByBusinessUnitCode(warehouseBuc) == null ? 0 : (existingForProduct.stream().map(e -> e.warehouseBusinessUnitCode).distinct().anyMatch(b -> b.equals(warehouseBuc)) ? 0 : 1));
    // The above check simplified: we already ensured the target warehouse is active and not present in existingForProduct (duplicate checked), so just increment
    if (distinctForProduct >= 2) {
      throw new WebApplicationException("product already associated with maximum number of warehouses for this store", 400);
    }

    // Constraint 2: store max 3 warehouses (distinct active warehouses)
    var existingForStore = fulfilmentRepository.findByStore(storeId);
    long distinctWarehousesActive = existingForStore.stream()
        .map(e -> e.warehouseBusinessUnitCode)
        .distinct()
        .filter(buc -> warehouseRepository.findByBusinessUnitCode(buc) != null)
        .count();
    boolean targetAlreadyForStore = existingForStore.stream().map(e -> e.warehouseBusinessUnitCode).distinct().anyMatch(b -> b.equals(warehouseBuc));
    long distinctAfterStore = distinctWarehousesActive + (targetAlreadyForStore ? 0 : 1);
    if (distinctAfterStore > 3) {
      throw new WebApplicationException("store already associated with maximum number of warehouses", 400);
    }

    // Constraint 3: warehouse max 5 products (distinct)
    var existingForWarehouse = fulfilmentRepository.findByWarehouse(warehouseBuc);
    long distinctProducts = existingForWarehouse.stream().map(e -> e.productId).distinct().count();
    boolean productAlreadyInWarehouse = existingForWarehouse.stream().map(e -> e.productId).distinct().anyMatch(pid -> pid.equals(productId));
    if (!productAlreadyInWarehouse && distinctProducts >= 5) {
      throw new WebApplicationException("warehouse already associated with maximum number of products", 400);
    }

    // create association
    var entity = new FulfilmentEntity(productId, storeId, warehouseBuc);
    fulfilmentRepository.persist(entity);
  }
}
