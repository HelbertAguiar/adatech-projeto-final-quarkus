package studio.catalogo.catalogoremoto.domain;

import java.util.List;

/** Resultado da consulta ao estoque, sem dependência do contrato REST de saída. */
public record CatalogoResultado(List<ProdutoDisponivel> products, String origem) {

    public record ProdutoDisponivel(Long id, String name, Integer stock) {}
}
