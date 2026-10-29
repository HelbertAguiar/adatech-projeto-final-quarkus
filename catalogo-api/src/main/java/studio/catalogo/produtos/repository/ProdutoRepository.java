package studio.catalogo.produtos.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;

/** Panache oferece findById, listAll, persist, deleteById, count e find. */
@ApplicationScoped
public class ProdutoRepository implements PanacheRepository<ProdutoEntity> {

    // Apenas a consulta por nome precisa de um metodo especifico.
    public List<ProdutoEntity> buscarPorNome(String trecho) {
        String filtro = "%" + trecho.trim().toLowerCase(Locale.ROOT) + "%";
        return find("lower(nome) like ?1 order by id", filtro).list();
    }
}
