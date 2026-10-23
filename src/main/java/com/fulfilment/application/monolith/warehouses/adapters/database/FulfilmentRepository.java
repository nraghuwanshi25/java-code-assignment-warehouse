package com.fulfilment.application.monolith.warehouses.adapters.database;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfilmentRepository implements PanacheRepository<FulfilmentEntity> {

  public List<FulfilmentEntity> findByProductAndStore(Long productId, Long storeId) {
    return list("productId = ?1 and storeId = ?2", productId, storeId);
  }

  public List<FulfilmentEntity> findByStore(Long storeId) {
    return list("storeId = ?1", storeId);
  }

  public List<FulfilmentEntity> findByWarehouse(String buc) {
    return list("warehouseBusinessUnitCode = ?1", buc);
  }
}

