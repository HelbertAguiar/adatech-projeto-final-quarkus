package studio.agendamento.agendamentos.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;

@ApplicationScoped
public class AgendamentoRepository implements PanacheRepository<AgendamentoEntity> {
    public boolean existeConflito(Long sala, LocalDate data, PeriodoEnum periodo, Long ignorarId) {
        if (ignorarId == null) {
            return count("id_sala = ?1 and data = ?2 and periodo = ?3", sala, data, periodo) > 0;
        }
        return count("id_sala = ?1 and data = ?2 and periodo = ?3 and id <> ?4",
                sala, data, periodo, ignorarId) > 0;
    }
}
