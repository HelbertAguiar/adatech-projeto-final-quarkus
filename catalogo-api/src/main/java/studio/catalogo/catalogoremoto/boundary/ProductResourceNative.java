package studio.catalogo.catalogoremoto.boundary;

import studio.catalogo.catalogoremoto.domain.ProductService;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * Variante com tipo de retorno explícito para que o Quarkus gere a serialização
 * de JSON durante o build da Native Image.
 */
@Path("/products-native")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResourceNative {

    @Inject
    ProductService productService;

    @Inject
    MeterRegistry meterRegistry;

    @GET
    public RestResponse<CatalogoResponse> getProducts() {
        CatalogoResponse catalogo = CatalogoResponse.from(productService.getProducts());

        meterRegistry.counter("catalogo.consultas", "origem", catalogo.origem()).increment();

        if ("FALLBACK".equals(catalogo.origem())) {
            return RestResponse.status(RestResponse.Status.SERVICE_UNAVAILABLE, catalogo);
        }
        return RestResponse.ok(catalogo);
    }
}
