package studio.catalogo.produtos.domain;

import lombok.RequiredArgsConstructor;
import studio.catalogo.produtos.repository.ProdutoEntity;
import studio.catalogo.produtos.repository.ProdutoRepository;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Coordena operações de negócio e define os limites das transações. */
@ApplicationScoped
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository repository;

    public List<ProdutoBo> listar(String nome) {
        List<ProdutoEntity> entidades = (nome == null || nome.isBlank())
                ? repository.listAll(Sort.by("id"))
                : repository.buscarPorNome(nome);
        return entidades.stream().map(ProdutoService::toDomain).toList();
    }

    public ProdutoBo buscar(Long id) {
        ProdutoEntity entity = repository.findById(id);
        if (entity == null) {
            throw new ProdutoNaoEncontradoException(id);
        }
        return toDomain(entity);
    }

    @Transactional
    public ProdutoBo criar(String nome, BigDecimal preco, int quantidade) {
        ProdutoEntity entity = new ProdutoEntity(nome.trim(), preco, quantidade);
        repository.persist(entity);
        return toDomain(entity);
    }

    @Transactional
    public ProdutoBo atualizar(Long id, String nome, BigDecimal preco, int quantidade) {
        ProdutoEntity entity = repository.findById(id);
        if (entity == null) {
            throw new ProdutoNaoEncontradoException(id);
        }
        // Dirty checking: Hibernate atualiza o registro no commit da transação.
        entity.nome = nome.trim();
        entity.preco = preco;
        entity.quantidade = quantidade;
        return toDomain(entity);
    }

    @Transactional
    public boolean excluir(Long id) {
        if (!repository.deleteById(id)) {
            throw new ProdutoNaoEncontradoException(id);
        }
        return true;
    }

    /** Aula 06: combina Bean Validation (no Resource) e exceção de negócio (409). */
    @Transactional
    public ProdutoBo retirarEstoque(Long id, int quantidade) {
        ProdutoEntity entity = repository.findById(id);
        if (entity == null) {
            throw new ProdutoNaoEncontradoException(id);
        }
        if (entity.quantidade < quantidade) {
            throw new EstoqueInsuficienteException(entity.quantidade, quantidade);
        }
        entity.quantidade -= quantidade; // Dirty checking do Hibernate na transação.
        return toDomain(entity);
    }

    private static ProdutoBo toDomain(ProdutoEntity entity) {
        return new ProdutoBo(entity.id, entity.nome, entity.preco, entity.quantidade);
    }
}
