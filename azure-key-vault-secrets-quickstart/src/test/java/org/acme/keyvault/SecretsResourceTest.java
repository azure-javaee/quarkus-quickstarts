package org.acme.keyvault;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Integration tests for Azure Key Vault Secret resources.
 * 
 * These tests are enabled only when the 'azure.test' system property is set to 'true'
 * to avoid failures when running without proper Azure Key Vault configuration.
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "azure.test", matches = "true")
public class SecretsResourceTest {

    private static final String TEST_SECRET_NAME = "test-secret-" + System.currentTimeMillis();
    private static final String TEST_SECRET_VALUE = "test-secret-value";

    /**
     * Test the complete lifecycle of a secret:
     * 1. Create a new secret
     * 2. Retrieve the secret
     * 3. Delete the secret
     */
    @Test
    public void testSecretLifecycle() {
        // Create a secret
        given()
            .contentType("application/json")
            .body(new SecretRequest(TEST_SECRET_NAME, TEST_SECRET_VALUE))
        .when()
            .post("/secrets/sync")
        .then()
            .statusCode(201)
            .body("name", is(TEST_SECRET_NAME))
            .body("value", is(TEST_SECRET_VALUE));

        // Get the secret
        given()
        .when()
            .get("/secrets/sync/" + TEST_SECRET_NAME)
        .then()
            .statusCode(200)
            .body("name", is(TEST_SECRET_NAME))
            .body("value", is(TEST_SECRET_VALUE));

        // List secrets (should include our test secret)
        given()
        .when()
            .get("/secrets/sync")
        .then()
            .statusCode(200)
            .body(notNullValue());

        // Delete the secret
        given()
        .when()
            .delete("/secrets/sync/" + TEST_SECRET_NAME)
        .then()
            .statusCode(200)
            .body("message", notNullValue());
    }

    /**
     * Test configuration-based secret access
     */
    @Test
    public void testConfigSecret() {
        given()
        .when()
            .get("/secrets/config/example")
        .then()
            .statusCode(200)
            .body("name", is("example-secret"))
            .body("value", notNullValue());
    }
}