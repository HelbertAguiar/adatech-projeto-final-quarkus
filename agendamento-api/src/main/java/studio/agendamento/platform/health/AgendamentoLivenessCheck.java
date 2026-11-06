package studio.agendamento.platform.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.*;

@Liveness
@ApplicationScoped
public class AgendamentoLivenessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("agendamento-vivo");
    }
}
