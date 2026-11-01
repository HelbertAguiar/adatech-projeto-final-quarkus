package studio.agendamento.platform.error.mapper;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import studio.agendamento.agendamentos.domain.AgendamentoNaoEncontradoException;
import studio.agendamento.platform.error.ErroHttp;

@Provider
public class AgendamentoNaoEncontradoMapper implements ExceptionMapper<AgendamentoNaoEncontradoException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(AgendamentoNaoEncontradoException exception) {
        return ErroHttp.resposta(404, "AGENDAMENTO_NAO_ENCONTRADO", exception.getMessage(), uriInfo.getPath());
    }
}
