package studio.catalogo.catalogoremoto.boundary;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(EstoqueTestResource.class)
class ProductResourceTest {

    @Test
    void deveFiltrarEstoqueEManterTotalCorreto() {
        given().when().get("/products").then()
                .statusCode(200)
                .body("total", is(2))
                .body("products.size()", is(2))
                .body("products[0].name", is("Caderno"))
                .body("origem", is("ESTOQUE"));
    }

    @Test
    void deveUtilizarFallbackQuandoEstoqueIndisponivel() {
        EstoqueTestResource.setIndisponivel(true);
        try {
            given().when().get("/products").then()
                    .statusCode(503)
                    .body("origem", is("FALLBACK"))
                    .body("total", is(0));
        } finally {
            EstoqueTestResource.setIndisponivel(false);
        }
    }

    @Test
    void deveExporMicroProfileConfig() {
        given().when().get("/info").then()
                .statusCode(200)
                .body("nome", is("Catálogo API"))
                .body("tipo", is("microservico"));
    }

    @Test
    void deveExporHealthChecks() {
        given().when().get("/q/health/live").then()
                .statusCode(200).body("status", is("UP"));
        given().when().get("/q/health/ready").then()
                .statusCode(200).body("status", is("UP"));
    }

    @Test
    void deveExporMetricas() {
        given().when().get("/products").then().statusCode(200);
        given().when().get("/q/metrics").then()
                .statusCode(200)
                .body(containsString("catalogo_consultas_total"));
    }
}
