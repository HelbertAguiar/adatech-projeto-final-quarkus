package studio.catalogo.architecture;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import studio.catalogo.catalogoremoto.boundary.ProductResource;
import studio.catalogo.catalogoremoto.domain.CatalogoResultado;
import studio.catalogo.catalogoremoto.domain.ProductService;
import studio.catalogo.produtos.boundary.ProdutoResource;
import studio.catalogo.produtos.domain.ProdutoBo;
import studio.catalogo.produtos.domain.ProdutoService;
import studio.catalogo.produtos.repository.ProdutoRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Teste simples de arquitetura, sem nova biblioteca: garante limites básicos do exemplo BDR. */
class OrganizacaoPacotesTest {

    @Test
    void pacotesSeguemFeatureMaisBoundaryDomainRepository() {
        assertEquals("studio.catalogo.produtos.boundary", ProdutoResource.class.getPackageName());
        assertEquals("studio.catalogo.produtos.domain", ProdutoService.class.getPackageName());
        assertEquals("studio.catalogo.produtos.repository", ProdutoRepository.class.getPackageName());
        assertEquals("studio.catalogo.catalogoremoto.boundary", ProductResource.class.getPackageName());
        assertEquals("studio.catalogo.catalogoremoto.domain", ProductService.class.getPackageName());
    }

    @Test
    void servicosDeDominioNaoRetornamDtoHttp() throws NoSuchMethodException {
        assertEquals(ProdutoBo.class,
                ProdutoService.class.getMethod("criar", String.class, BigDecimal.class, int.class).getReturnType());
        assertEquals(CatalogoResultado.class,
                ProductService.class.getMethod("getProducts").getReturnType());
        assertNull(ProdutoBo.class.getAnnotation(Entity.class), "O modelo de domínio não deve ser entidade JPA");
    }
}
