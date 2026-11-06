package studio.agendamento.platform.error.mapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import studio.agendamento.platform.error.ErroHttp;

/** Inclui JSON malformado, enum/data inválidos e parâmetros não conversíveis. */
@Provider
public class RequisicaoInvalidaMapper implements ExceptionMapper<WebApplicationException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse().getStatus();
        return ErroHttp.resposta(status, status == 400 ? "DADOS_INVALIDOS" : "HTTP_" + status,
                status == 400 ? "Corpo ou parâmetros da requisição inválidos."
                        : "Não foi possível atender à requisição.", uriInfo.getPath());
    }
}
