package studio.catalogo.produtos.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Aula 05: cada objeto ProdutoEntity representa uma linha da tabela produtos. */
@Entity
@Table(name = "produtos")
public class ProdutoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 120)
    public String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal preco;

    @Column(nullable = false)
    public int quantidade;

    // Construtor sem argumentos: exigido pelo Jakarta Persistence.
    public ProdutoEntity() {
    }

    public ProdutoEntity(String nome, BigDecimal preco, int quantidade) {
        this.nome = nome;
        this.preco = preco;
        this.quantidade = quantidade;
    }
}
