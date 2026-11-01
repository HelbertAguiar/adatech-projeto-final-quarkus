package studio.agendamento.platform;

import java.util.Map;
import io.quarkus.test.junit.*;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestProfile(StagingReadinessTest.Profile.class)
class StagingReadinessTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() { return "staging"; }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("agendamento.pronto", "false",
                    "quarkus.datasource.jdbc.url", "jdbc:h2:mem:staging-test;DB_CLOSE_DELAY=-1",
                    "quarkus.hibernate-orm.schema-management.strategy", "drop-and-create");
        }
    }

    @Test
    void stagingPodeFicarNaoProntoSemFalharLiveness() {
        given().get("/info").then().statusCode(200)
                .body("nome", is("Agendamento de Massagem - STAGING"));
        given().get("/q/health/live").then().statusCode(200).body("status", is("UP"));
        given().get("/q/health/ready").then().statusCode(503).body("status", is("DOWN"));
    }
}
