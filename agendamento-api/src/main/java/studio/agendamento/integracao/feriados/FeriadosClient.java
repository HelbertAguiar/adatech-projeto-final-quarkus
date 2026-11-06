package studio.agendamento.integracao.feriados;

import java.time.LocalDate;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/feriados")
@RegisterRestClient(configKey = "feriados")
@Produces(MediaType.APPLICATION_JSON)
public interface FeriadosClient {
    @GET
    FeriadoResponse consultar(@QueryParam("uf") String uf, @QueryParam("data") LocalDate data);
}
