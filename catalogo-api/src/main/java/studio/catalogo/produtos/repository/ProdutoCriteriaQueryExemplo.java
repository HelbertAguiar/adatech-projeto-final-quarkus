package studio.catalogo.produtos.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Apenas exemplo didático de JPA Criteria API, sem endpoint REST.
 * Mostra como montar filtros opcionais sem concatenar fragmentos de JPQL.
 * A implementação do CRUD da aula continua sendo PanacheRepository.
 */
@ApplicationScoped
public class ProdutoCriteriaQueryExemplo {

    private final EntityManager entityManager;

    public ProdutoCriteriaQueryExemplo(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<ProdutoEntity> buscar(String nome, BigDecimal precoMin, Integer quantidadeMin) {
        // 1. Obtem o construtor da consulta a partir do EntityManager.
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        // 2. Define que a consulta devolvera entidades ProdutoEntity.
        CriteriaQuery<ProdutoEntity> query = cb.createQuery(ProdutoEntity.class);
        // 3. Define a tabela/entidade que sera pesquisada.
        Root<ProdutoEntity> root = query.from(ProdutoEntity.class);
        List<Predicate> filtros = new ArrayList<>();

        if (nome != null && !nome.isBlank()) {
            filtros.add(cb.like(cb.lower(root.<String>get("nome")),
                    "%" + nome.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
        }
        if (precoMin != null) {
            filtros.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("preco"), precoMin));
        }
        if (quantidadeMin != null) {
            filtros.add(cb.ge(root.<Integer>get("quantidade"), quantidadeMin));
        }

        // 4. Aplica os filtros e ordena o resultado.
        query.select(root).where(filtros.toArray(Predicate[]::new));
        query.orderBy(cb.asc(root.get("id")));
        // 5. Executa a consulta e devolve a lista encontrada.
        return entityManager.createQuery(query).getResultList();
    }
}
