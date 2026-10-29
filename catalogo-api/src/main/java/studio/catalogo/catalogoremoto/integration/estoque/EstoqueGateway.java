package studio.catalogo.catalogoremoto.integration.estoque;

import studio.catalogo.catalogoremoto.integration.estoque.Catalogo;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/products")
@RegisterRestClient(configKey = "estoque-local")
public interface EstoqueGateway {
    @GET
    Catalogo products();
}


