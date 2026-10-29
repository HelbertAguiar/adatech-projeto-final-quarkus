package studio.catalogo.platform.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class CatalogoReadinessCheck implements HealthCheck {

    @ConfigProperty(name = "catalogo.nome")
    String nome;

    @Override
    public HealthCheckResponse call() {
        // Exemplo didático: o serviço está configurado e pronto para responder.
        // Não fazemos chamadas REST aqui para não derrubar todas as réplicas
        // caso a dependência externa apresente indisponibilidade temporária.
        return HealthCheckResponse.named("catalogo-configurado")
                .status(nome != null && !nome.isBlank())
                .build();
    }
}
