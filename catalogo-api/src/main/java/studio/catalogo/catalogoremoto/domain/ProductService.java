package studio.catalogo.catalogoremoto.domain;

import studio.catalogo.catalogoremoto.integration.estoque.EstoqueGateway;
import studio.catalogo.catalogoremoto.integration.estoque.Catalogo;
import studio.catalogo.catalogoremoto.domain.CatalogoResultado.ProdutoDisponivel;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

/** Domínio do catálogo remoto: filtra produtos e define resposta de fallback. */
@ApplicationScoped
public class ProductService {

    private final EstoqueGateway estoqueGateway;

    public ProductService(@RestClient EstoqueGateway estoqueGateway) {
        this.estoqueGateway = estoqueGateway;
    }

    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackProducts")
    public CatalogoResultado getProducts() {
        Catalogo catalogoExterno = estoqueGateway.products();

        // Consulta sem estado: retry não acumula resultados repetidos.
        List<ProdutoDisponivel> disponiveis = catalogoExterno.products().stream()
                .filter(product -> product.stock() > 0)
                .map(product -> new ProdutoDisponivel((long) product.id(), product.title(), product.stock()))
                .toList();

        return new CatalogoResultado(disponiveis, "ESTOQUE");
    }

    // Não simular uma consulta bem-sucedida em caso de falha da dependência externa.
    public CatalogoResultado fallbackProducts() {
        return new CatalogoResultado(List.of(), "FALLBACK");
    }
}
