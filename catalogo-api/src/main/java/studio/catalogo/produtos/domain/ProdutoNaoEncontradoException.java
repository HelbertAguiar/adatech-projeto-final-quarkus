package studio.catalogo.produtos.domain;

/** Exceção de negócio sem qualquer dependência de HTTP ou JAX-RS. */
public class ProdutoNaoEncontradoException extends RuntimeException {
    public ProdutoNaoEncontradoException(Long id) {
        super("Produto " + id + " não encontrado.");
    }
}
