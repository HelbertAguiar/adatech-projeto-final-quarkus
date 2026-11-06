package studio.agendamento.platform.error.mapper;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import studio.agendamento.platform.error.ErroHttp;

@Provider
@Priority(Priorities.USER - 1)
public class FormatoJsonInvalidoMapper implements ExceptionMapper<MismatchedInputException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(MismatchedInputException exception) {
        return ErroHttp.resposta(400, "DADOS_INVALIDOS", "JSON inválido ou campo com formato incompatível.", uriInfo.getPath());
    }
}
