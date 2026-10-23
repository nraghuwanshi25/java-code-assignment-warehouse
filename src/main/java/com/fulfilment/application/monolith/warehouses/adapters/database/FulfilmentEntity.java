package com.fulfilment.application.monolith.warehouses.adapters.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "fulfilment")
public class FulfilmentEntity {

  @Id @GeneratedValue public Long id;

  @Column(nullable = false) public Long productId;

  @Column(nullable = false) public Long storeId;

  @Column(length = 100, nullable = false) public String warehouseBusinessUnitCode;

  public FulfilmentEntity() {}

  public FulfilmentEntity(Long productId, Long storeId, String warehouseBusinessUnitCode) {
    this.productId = productId;
    this.storeId = storeId;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
  }
}

