package studio.catalogo.produtos.domain;

/** A retirada não pode deixar o saldo do estoque negativo. */
public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException(int disponivel, int solicitado) {
        super("Estoque insuficiente: disponível=" + disponivel + ", solicitado=" + solicitado + ".");
    }
}
