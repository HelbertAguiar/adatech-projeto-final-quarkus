package studio.agendamento.platform.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.*;

@Readiness
@ApplicationScoped
public class AgendamentoReadinessCheck implements HealthCheck {
    @ConfigProperty(name = "agendamento.pronto")
    boolean pronto;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("agendamento-habilitado").status(pronto).build();
    }
}
