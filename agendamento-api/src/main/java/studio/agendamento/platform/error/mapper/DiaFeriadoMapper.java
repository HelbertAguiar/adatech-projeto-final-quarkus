package studio.agendamento.platform.error.mapper;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import studio.agendamento.agendamentos.domain.DiaFeriadoException;
import studio.agendamento.platform.error.ErroHttp;

@Provider
public class DiaFeriadoMapper implements ExceptionMapper<DiaFeriadoException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(DiaFeriadoException exception) {
        return ErroHttp.resposta(409, "DIA_FERIADO", exception.getMessage(), uriInfo.getPath());
    }
}
