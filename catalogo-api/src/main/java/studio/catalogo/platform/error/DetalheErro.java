package studio.catalogo.platform.error;

/** Detalhe de uma restrição Bean Validation. */
public record DetalheErro(String campo, String mensagem) {
}
