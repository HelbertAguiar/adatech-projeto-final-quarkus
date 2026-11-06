package studio.agendamento.platform.info;

import java.util.Map;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/info")
@Produces(MediaType.APPLICATION_JSON)
public class AgendamentoInfoResource {
    @ConfigProperty(name = "agendamento.nome")
    String nome;

    @GET
    public Map<String, String> info() {
        return Map.of("nome", nome);
    }
}
