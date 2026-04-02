package io.github.hectorvent.floci.lifecycle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class HealthControllerIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void healthEndpoint_returnsExpectedJson() throws Exception {
        String body = given()
            .when()
                .get("/_floci/health")
            .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().asString();

        JsonNode actual = MAPPER.readTree(body);

        Map<String, Object> expected = new LinkedHashMap<>();
        Map<String, String> services = new LinkedHashMap<>();
        services.put("ssm", "running");
        services.put("sqs", "running");
        services.put("s3", "running");
        services.put("dynamodb", "running");
        services.put("sns", "running");
        services.put("lambda", "running");
        services.put("apigateway", "running");
        services.put("iam", "running");
        services.put("elasticache", "running");
        services.put("rds", "running");
        services.put("events", "running");
        services.put("logs", "running");
        services.put("monitoring", "running");
        services.put("secretsmanager", "running");
        services.put("apigatewayv2", "running");
        services.put("kinesis", "running");
        services.put("kms", "running");
        services.put("cognito-idp", "running");
        services.put("states", "running");
        services.put("cloudformation", "running");
        services.put("acm", "running");
        services.put("email", "running");
        services.put("es", "running");
        expected.put("services", services);
        expected.put("edition", "floci-always-free");
        expected.put("version", "dev");

        assertEquals(MAPPER.valueToTree(expected), actual);
    }

    @Test
    void healthEndpoint_localstackCompatPath() throws Exception {
        String body = given()
            .when()
                .get("/_localstack/health")
            .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().asString();

        JsonNode actual = MAPPER.readTree(body);

        assertNotNull(actual.get("services"));
        assertEquals("floci-always-free", actual.get("edition").asText());
        assertNotNull(actual.get("version"));
        // Verify same structure as /_floci/health
        assertEquals(23, actual.get("services").size());
    }
}
