package studio.catalogo.platform.error.mapper;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import studio.catalogo.platform.error.DetalheErro;
import studio.catalogo.platform.error.ErroHttp;

import java.util.Comparator;
import java.util.List;

/** Mapeia SOMENTE violações de parâmetros HTTP (400), não erros internos do serviço (500). */
@Provider
public class ValidacaoExceptionMapper implements ExceptionMapper<ResteasyReactiveViolationException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ResteasyReactiveViolationException exception) {
        List<DetalheErro> detalhes = exception.getConstraintViolations().stream()
                .map(ValidacaoExceptionMapper::detalhe)
                .sorted(Comparator.comparing(DetalheErro::campo).thenComparing(DetalheErro::mensagem))
                .toList();
        return ErroHttp.resposta(400, "DADOS_INVALIDOS", "Verifique os dados enviados.",
                uriInfo.getPath(), detalhes);
    }

    private static DetalheErro detalhe(ConstraintViolation<?> violacao) {
        String caminho = violacao.getPropertyPath().toString();
        String campo = caminho.substring(caminho.lastIndexOf('.') + 1);
        return new DetalheErro(campo, violacao.getMessage());
    }
}
