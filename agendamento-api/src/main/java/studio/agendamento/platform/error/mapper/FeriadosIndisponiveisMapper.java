package studio.agendamento.platform.error.mapper;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import studio.agendamento.integracao.feriados.FeriadosIndisponiveisException;
import studio.agendamento.platform.error.ErroHttp;

@Provider
public class FeriadosIndisponiveisMapper implements ExceptionMapper<FeriadosIndisponiveisException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(FeriadosIndisponiveisException exception) {
        return ErroHttp.resposta(503, "FERIADOS_INDISPONIVEIS", exception.getMessage(), uriInfo.getPath());
    }
}
