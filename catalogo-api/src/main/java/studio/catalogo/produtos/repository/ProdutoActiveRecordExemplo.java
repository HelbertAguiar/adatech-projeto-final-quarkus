// Exemplo para estudo: propositalmente fora de src/main/java.
// NÃO habilitar ao mesmo tempo que ProdutoEntity usando a tabela "produtos".
// PanacheEntity cria seu próprio campo Long id e estratégia de geração de IDs.
package studio.catalogo.produtos.repository;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

// @Entity                // Descomentar APENAS ao substituir a entidade Repository
// @Table(name = "produtos") // Necessário adequar a estratégia de ID/schema antes de ativar
public class ProdutoActiveRecordExemplo extends PanacheEntity {
    // PanacheEntity já oferece o atributo público Long id (@Id + @GeneratedValue).
    @Column(nullable = false, length = 120)
    public String nome;
    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal preco;
    @Column(nullable = false)
    public int quantidade;

    public static List<ProdutoActiveRecordExemplo> buscarPorNome(String trecho) {
        return list("lower(nome) like ?1 order by id",
                "%" + trecho.toLowerCase(Locale.ROOT).trim() + "%");
    }

    // Exemplos de uso dentro de um @ApplicationScoped @Transactional service:
    // var p = new ProdutoActiveRecordExemplo();
    // p.nome = "Mochila"; p.preco = new BigDecimal("79.90"); p.quantidade = 4;
    // p.persist();                    // CREATE
    // var encontrado = ProdutoActiveRecordExemplo.findById(p.id); // READ
    // encontrado.quantidade = 5;      // UPDATE via dirty checking (transação)
    // ProdutoActiveRecordExemplo.deleteById(p.id); // DELETE
    // var todos = ProdutoActiveRecordExemplo.listAll(); // LIST
}
