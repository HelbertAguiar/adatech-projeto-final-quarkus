package studio.agendamento.platform.error.mapper;

import java.util.Comparator;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import studio.agendamento.platform.error.*;

@Provider
@Priority(Priorities.USER - 1)
public class ValidacaoExceptionMapper implements ExceptionMapper<ResteasyReactiveViolationException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ResteasyReactiveViolationException exception) {
        var detalhes = exception.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    return new DetalheErro(path.substring(path.lastIndexOf('.') + 1), v.getMessage());
                })
                .sorted(Comparator.comparing(DetalheErro::campo).thenComparing(DetalheErro::mensagem)).toList();
        return ErroHttp.resposta(400, "DADOS_INVALIDOS", "Verifique os dados enviados.", uriInfo.getPath(), detalhes);
    }
}
