package studio.agendamento.platform.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import studio.agendamento.agendamentos.domain.AgendamentoCriado;

@Startup
@ApplicationScoped
public class AgendamentoMetrics {
    private final Counter criados;

    public AgendamentoMetrics(MeterRegistry registry) {
        criados = registry.counter("agendamentos.criados");
    }

    public void registrar(@Observes(during = TransactionPhase.AFTER_SUCCESS) AgendamentoCriado evento) {
        criados.increment();
    }
}
