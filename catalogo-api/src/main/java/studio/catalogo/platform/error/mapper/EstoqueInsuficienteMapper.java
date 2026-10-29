package studio.catalogo.platform.error.mapper;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import studio.catalogo.platform.error.ErroHttp;
import studio.catalogo.produtos.domain.EstoqueInsuficienteException;

@Provider
public class EstoqueInsuficienteMapper implements ExceptionMapper<EstoqueInsuficienteException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(EstoqueInsuficienteException exception) {
        return ErroHttp.resposta(409, "ESTOQUE_INSUFICIENTE", exception.getMessage(),
                uriInfo.getPath());
    }
}
