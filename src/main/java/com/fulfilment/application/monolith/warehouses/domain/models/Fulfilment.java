package com.fulfilment.application.monolith.warehouses.domain.models;

public class Fulfilment {
  public Long productId;
  public Long storeId;
  public String warehouseBusinessUnitCode; // association via BUC

  public Fulfilment() {}

  public Fulfilment(Long productId, Long storeId, String warehouseBusinessUnitCode) {
    this.productId = productId;
    this.storeId = storeId;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
  }
}

