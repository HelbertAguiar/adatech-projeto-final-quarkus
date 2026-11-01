package studio.agendamento.platform.error.mapper;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import studio.agendamento.agendamentos.domain.ReservaConflitanteException;
import studio.agendamento.platform.error.ErroHttp;

@Provider
public class ReservaConflitanteMapper implements ExceptionMapper<ReservaConflitanteException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ReservaConflitanteException exception) {
        return ErroHttp.resposta(409, "RESERVA_CONFLITANTE", exception.getMessage(), uriInfo.getPath());
    }
}
