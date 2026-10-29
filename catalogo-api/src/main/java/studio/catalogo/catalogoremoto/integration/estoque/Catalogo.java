package studio.catalogo.catalogoremoto.integration.estoque;

import java.util.List;

public record Catalogo(List<Product> products, int total) {}
