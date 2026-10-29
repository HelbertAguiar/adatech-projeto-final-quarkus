package studio.catalogo.platform.error.mapper;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import studio.catalogo.platform.error.ErroHttp;
import studio.catalogo.produtos.domain.ProdutoNaoEncontradoException;

@Provider
public class ProdutoNaoEncontradoMapper implements ExceptionMapper<ProdutoNaoEncontradoException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ProdutoNaoEncontradoException exception) {
        return ErroHttp.resposta(404, "PRODUTO_NAO_ENCONTRADO", exception.getMessage(),
                uriInfo.getPath());
    }
}
