package studio.agendamento.agendamentos.domain;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import studio.agendamento.agendamentos.repository.*;
import studio.agendamento.integracao.feriados.FeriadosService;

@ApplicationScoped
@RequiredArgsConstructor
public class AgendamentoService {
    private final AgendamentoRepository repository;
    private final FeriadosService feriados;
    private final Event<AgendamentoCriado> criado;

    public List<AgendamentoBo> listar() {
        return repository.listAll(Sort.by("id")).stream().map(AgendamentoService::toDomain).toList();
    }

    public AgendamentoBo buscar(Long id) {
        return toDomain(encontrar(id));
    }

    @Transactional
    public AgendamentoBo criar(Long sala, Long usuario, String uf, LocalDate data, PeriodoEnum periodo) {
        validarReserva(sala, uf, data, periodo, null);
        AgendamentoEntity entity = new AgendamentoEntity();
        preencher(entity, sala, usuario, uf, data, periodo);
        try {
            repository.persistAndFlush(entity);
        } catch (PersistenceException exception) {
            throw traduzirConflito(exception);
        }
        criado.fire(new AgendamentoCriado(entity.id));
        return toDomain(entity);
    }

    @Transactional
    public AgendamentoBo atualizar(Long id, Long sala, Long usuario, String uf, LocalDate data, PeriodoEnum periodo) {
        AgendamentoEntity entity = encontrar(id);
        validarReserva(sala, uf, data, periodo, id);
        preencher(entity, sala, usuario, uf, data, periodo);
        try {
            repository.flush(); // dirty checking; força a verificação da constraint antes da resposta.
        } catch (PersistenceException exception) {
            throw traduzirConflito(exception);
        }
        return toDomain(entity);
    }

    @Transactional
    public void excluir(Long id) {
        if (!repository.deleteById(id)) {
            throw new AgendamentoNaoEncontradoException(id);
        }
    }

    private void validarReserva(Long sala, String uf, LocalDate data, PeriodoEnum periodo, Long ignorarId) {
        if (repository.existeConflito(sala, data, periodo, ignorarId)) {
            throw new ReservaConflitanteException();
        }
        if (feriados.ehFeriado(uf, data)) {
            throw new DiaFeriadoException();
        }
    }

    private AgendamentoEntity encontrar(Long id) {
        AgendamentoEntity entity = repository.findById(id);
        if (entity == null) {
            throw new AgendamentoNaoEncontradoException(id);
        }
        return entity;
    }

    private static void preencher(AgendamentoEntity entity, Long sala, Long usuario, String uf,
                                  LocalDate data, PeriodoEnum periodo) {
        entity.id_sala = sala;
        entity.id_usuario = usuario;
        entity.uf = uf;
        entity.data = data;
        entity.periodo = periodo;
    }

    private static RuntimeException traduzirConflito(PersistenceException exception) {
        for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                return new ReservaConflitanteException();
            }
        }
        return exception;
    }

    private static AgendamentoBo toDomain(AgendamentoEntity entity) {
        return new AgendamentoBo(entity.id, entity.id_sala, entity.id_usuario,
                entity.uf, entity.data, entity.periodo);
    }
}
