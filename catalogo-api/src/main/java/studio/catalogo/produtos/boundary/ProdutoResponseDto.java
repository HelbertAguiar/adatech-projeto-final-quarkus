package studio.catalogo.produtos.boundary;

import studio.catalogo.produtos.domain.ProdutoBo;
import java.math.BigDecimal;

/** Contrato HTTP público: não expõe diretamente a entidade JPA. */
public record ProdutoResponseDto(Long id, String nome, BigDecimal preco, int quantidade) {

    public static ProdutoResponseDto from(ProdutoBo produtoBo) {
        return new ProdutoResponseDto(produtoBo.id(), produtoBo.nome(), produtoBo.preco(), produtoBo.quantidade());
    }
}
