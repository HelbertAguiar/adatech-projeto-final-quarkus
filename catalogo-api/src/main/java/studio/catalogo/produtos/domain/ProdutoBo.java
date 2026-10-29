package studio.catalogo.produtos.domain;

import java.math.BigDecimal;

/** Dados de negócio; não é um DTO HTTP nem uma entidade JPA. */
public record ProdutoBo(Long id, String nome, BigDecimal preco, int quantidade) {}
