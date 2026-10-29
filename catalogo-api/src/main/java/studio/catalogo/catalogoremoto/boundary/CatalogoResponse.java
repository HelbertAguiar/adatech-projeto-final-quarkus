package studio.catalogo.catalogoremoto.boundary;

import studio.catalogo.catalogoremoto.domain.CatalogoResultado;

import java.util.List;

/** Contrato HTTP de saída; não deve ser usado pelo serviço de domínio. */
public record CatalogoResponse(List<ProductDto> products, int total, String origem) {

    public static CatalogoResponse from(CatalogoResultado resultado) {
        List<ProductDto> products = resultado.products().stream()
                .map(p -> new ProductDto(p.id(), p.name(), p.stock()))
                .toList();
        return new CatalogoResponse(products, products.size(), resultado.origem());
    }
}
