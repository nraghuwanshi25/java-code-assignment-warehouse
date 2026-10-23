package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

public class LegacyStoreManagerGatewayTest {

  @Test
  public void testCreateStoreOnLegacySystemDoesNotThrow() {
    var gateway = new LegacyStoreManagerGateway();
    var store = new Store("TEST-STORE");
    store.quantityProductsInStock = 5;

    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  public void testUpdateStoreOnLegacySystemDoesNotThrow() {
    var gateway = new LegacyStoreManagerGateway();
    var store = new Store("TEST-STORE-2");
    store.quantityProductsInStock = 12;

    assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
  }

  @Test
  public void testWriteToFileHandlesExceptions() {
    var gateway = new LegacyStoreManagerGateway();
    // prefix shorter than 3 characters causes Files.createTempFile to throw IllegalArgumentException
    var badStore = new Store("ab");
    badStore.quantityProductsInStock = 1;

    // Exception should be caught inside writeToFile; ensure no exception propagates
    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(badStore));
  }
}
