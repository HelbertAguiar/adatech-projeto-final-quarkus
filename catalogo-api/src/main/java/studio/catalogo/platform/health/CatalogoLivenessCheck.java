package studio.catalogo.platform.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class CatalogoLivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        // Liveness testa a execução do processo, NÃO a disponibilidade do estoque.
        //return HealthCheckResponse.up("catalogo-vivo");
        return HealthCheckResponse.builder().name("catalogo-api").withData("version", 1L).up().build();
    }
}
