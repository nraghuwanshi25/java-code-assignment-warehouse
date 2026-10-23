package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
            .when()
            .get(path)
            .then()
            .statusCode(200)
            .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
            .when()
            .get(path)
            .then()
            .statusCode(200)
            .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void testGetSingleNotFound() {
    final String path = "product";
    given().when().get(path + "/99999").then().statusCode(404);
  }

  @Test
  public void testCreateWithIdRejected() {
    final String path = "product";
    String payload = "{\"id\":999,\"name\":\"BAD\",\"stock\":1}";
    given()
        .contentType("application/json")
        .body(payload)
        .when()
        .post(path)
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateValidationAndNotFound() {
    final String path = "product";

    // validation: missing name
    String bad = "{\"description\":\"no-name\",\"stock\":1}";
    given()
        .contentType("application/json")
        .body(bad)
        .when()
        .put(path + "/1")
        .then()
        .statusCode(422);

    // not found
    String ok = "{\"name\":\"OK\",\"stock\":1}";
    given()
        .contentType("application/json")
        .body(ok)
        .when()
        .put(path + "/99999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteNotFoundAndSuccess() {
    final String path = "product";

    // delete non-existent
    given().when().delete(path + "/99999").then().statusCode(404);

    // create and delete
    String payload = "{\"name\":\"TO-DEL\",\"stock\":1}";
    String created = given()
        .contentType("application/json")
        .body(payload)
        .when()
        .post(path)
        .then()
        .statusCode(201)
        .extract()
        .asString();

    int idPos = created.indexOf("\"id\":");
    String idStr = created.substring(idPos + 5, created.indexOf(',', idPos)).replaceAll("[^0-9]", "");

    given().when().delete(path + "/" + idStr).then().statusCode(204);
  }
}
