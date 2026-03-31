package io.github.hectorvent.floci.lifecycle;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class HealthControllerIntegrationTest {

    @Test
    void healthEndpoint_returnsVersionAndServices() {
        given()
        .when()
            .get("/_floci/health")
        .then()
            .statusCode(200)
            .contentType("application/json")
            .body("version", notNullValue())
            .body("edition", equalTo("community"))
            .body("services", notNullValue())
            .body("services.sqs", equalTo("available"))
            .body("services.s3", equalTo("available"))
            .body("services.dynamodb", equalTo("available"))
            .body("services.ssm", equalTo("available"))
            .body("services.sns", equalTo("available"))
            .body("services.lambda", equalTo("available"))
            .body("services.iam", equalTo("available"));
    }
}
