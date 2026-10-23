package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

@QuarkusTest
public class StoreResourceTest {


  @Test
  public void testListAndGetStore() {
    final String path = "store";

    // initial stores from import.sql
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // get single existing store (id 1 seeded)
    given()
        .when()
        .get(path + "/1")
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"));
  }

  @Test
  public void testCreateUpdatePatchDeleteStore() {
    final String path = "store";

    var newStore = new Store("UNIT-TEST");
    newStore.quantityProductsInStock = 7;

    // Create
    String created = given()
        .contentType("application/json")
        .body(newStore)
        .when()
        .post(path)
        .then()
        .statusCode(201)
        .extract()
        .asString();

    // extract id
    int idPos = created.indexOf("\"id\":");
    String idStr = created.substring(idPos + 5, created.indexOf(',', idPos)).replaceAll("[^0-9]", "");

    // Update
    Store upd = new Store("UPDATED-STORE");
    upd.quantityProductsInStock = 9;

    given()
        .contentType("application/json")
        .body(upd)
        .when()
        .put(path + "/" + idStr)
        .then()
        .statusCode(200)
        .body(containsString("UPDATED-STORE"));

    // Patch (partial)
    Store patch = new Store("PATCHED");
    patch.quantityProductsInStock = 10;
    given()
        .contentType("application/json")
        .body(patch)
        .when()
        .patch(path + "/" + idStr)
        .then()
        .statusCode(200)
        .body(containsString("PATCHED"));

    // Delete
    given()
        .when()
        .delete(path + "/" + idStr)
        .then()
        .statusCode(204);
  }

  @Test
  public void testCreateWithIdRejectedAndValidation() {
    final String path = "store";

    Store bad = new Store("BAD");
    bad.id = 999L;
    given()
        .contentType("application/json")
        .body(bad)
        .when()
        .post(path)
        .then()
        .statusCode(422);

    Store upd = new Store(); // missing name
    given()
        .contentType("application/json")
        .body(upd)
        .when()
        .put(path + "/1")
        .then()
        .statusCode(422);
  }

  @Test
  public void testDeleteNonexistent() {
    final String path = "store";
    given()
        .when()
        .delete(path + "/99999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testUpdateNonexistentReturns404() {
    final String path = "store";

    Store upd = new Store("NON-EXISTENT");
    upd.quantityProductsInStock = 3;

    given()
        .contentType("application/json")
        .body(upd)
        .when()
        .put(path + "/99999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testGetSingleNonexistentReturns404() {
    final String path = "store";
    given()
        .when()
        .get(path + "/99999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testPatchNonexistentReturns404() {
    final String path = "store";

    Store upd = new Store("PATCH-NON");
    upd.quantityProductsInStock = 4;

    given()
        .contentType("application/json")
        .body(upd)
        .when()
        .patch(path + "/99999")
        .then()
        .statusCode(404);
  }

  @Test
  public void runAfterCommit_executesOnlyOnCommittedStatus_and_handlesExceptions() throws Exception {
    StoreResource resource = new StoreResource();

    // Capture the registered synchronization
    final Synchronization[] captured = new Synchronization[1];

    TransactionSynchronizationRegistry fakeRegistry = new TransactionSynchronizationRegistry() {
      @Override
      public void registerInterposedSynchronization(Synchronization sync) {
        captured[0] = sync;
      }

      // Remaining methods are not used in this test - provide simple stubs
      @Override
      public Object getTransactionKey() {
        return null;
      }

      @Override
      public boolean getRollbackOnly() {
        return false;
      }

      @Override
      public void setRollbackOnly() {}

      @Override
      public int getTransactionStatus() {
        return 0;
      }

      @Override
      public void putResource(Object key, Object value) {}

      @Override
      public Object getResource(Object key) {
        return null;
      }
    };

    // Inject fake registry
    var trField = StoreResource.class.getDeclaredField("transactionRegistry");
    trField.setAccessible(true);
    trField.set(resource, fakeRegistry);

    // Case 1: action runs when committed
    AtomicBoolean ran = new AtomicBoolean(false);
    var runAfterCommitMethod = StoreResource.class.getDeclaredMethod("runAfterCommit", Runnable.class);
    runAfterCommitMethod.setAccessible(true);
    runAfterCommitMethod.invoke(resource, (Runnable) () -> ran.set(true));

    // Ensure synchronization was registered
    Synchronization sync = captured[0];
    assertTrue(sync != null);

    // Simulate afterCompletion with non-committed status
    sync.afterCompletion(Status.STATUS_ROLLEDBACK);
    assertFalse(ran.get(), "Action should not run when transaction not committed");

    // Simulate afterCompletion with committed status
    sync.afterCompletion(Status.STATUS_COMMITTED);
    assertTrue(ran.get(), "Action should run when transaction committed");

    // Case 2: action throws - ensure exception is caught and not propagated
    AtomicBoolean called = new AtomicBoolean(false);
    runAfterCommitMethod.invoke(resource, (Runnable) () -> { called.set(true); throw new RuntimeException("boom"); });
    Synchronization sync2 = captured[0];
    // Should not throw
    sync2.afterCompletion(Status.STATUS_COMMITTED);
    assertTrue(called.get(), "Action that throws should still have been invoked and handled");
  }

  @Test
  public void errorMapper_returnsCorrectPayloads() throws Exception {
    StoreResource.ErrorMapper mapper = new StoreResource.ErrorMapper();
    // inject ObjectMapper
    var omField = StoreResource.ErrorMapper.class.getDeclaredField("objectMapper");
    omField.setAccessible(true);
    omField.set(mapper, new ObjectMapper());

    // Generic exception -> 500
    Response r1 = mapper.toResponse(new Exception("bad"));
    assertEquals(500, r1.getStatus());
    ObjectNode body1 = (ObjectNode) r1.getEntity();
    assertEquals("java.lang.Exception", body1.get("exceptionType").asText());
    assertEquals(500, body1.get("code").asInt());
    assertEquals("bad", body1.get("error").asText());

    // WebApplicationException -> uses its status
    WebApplicationException wae = new WebApplicationException("not found", 404);
    Response r2 = mapper.toResponse(wae);
    assertEquals(404, r2.getStatus());
    ObjectNode body2 = (ObjectNode) r2.getEntity();
    assertEquals("jakarta.ws.rs.WebApplicationException", body2.get("exceptionType").asText());
    assertEquals(404, body2.get("code").asInt());
    assertEquals("not found", body2.get("error").asText());
  }
}
