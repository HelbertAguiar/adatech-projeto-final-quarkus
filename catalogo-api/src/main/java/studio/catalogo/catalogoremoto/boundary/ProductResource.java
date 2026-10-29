package studio.catalogo.catalogoremoto.boundary;

import studio.catalogo.catalogoremoto.domain.ProductService;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    @Inject
    ProductService productService;

    @Inject
    MeterRegistry meterRegistry;

    @GET
    public Response getProducts() {
        CatalogoResponse catalogo = CatalogoResponse.from(productService.getProducts());

        // Micrometer: contador de negócio exportado em /q/metrics.
        meterRegistry.counter("catalogo.consultas", "origem", catalogo.origem()).increment();

        if ("FALLBACK".equals(catalogo.origem())) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(catalogo).build();
        }
        return Response.ok(catalogo).build();
    }
}
