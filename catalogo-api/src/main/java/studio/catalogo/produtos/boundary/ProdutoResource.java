package studio.catalogo.produtos.boundary;

import lombok.RequiredArgsConstructor;
import studio.catalogo.produtos.domain.ProdutoBo;
import studio.catalogo.produtos.domain.ProdutoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;

@Path("/produtos")
@RequiredArgsConstructor
public class ProdutoResource {

    private final ProdutoService service;

    @GET
    public Response listar(@QueryParam("nome") String nome) {
        return Response.ok(service.listar(nome).stream().map(ProdutoResponseDto::from).toList()).build();
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) {
        return Response.ok(ProdutoResponseDto.from(service.buscar(id))).build();
    }

    @POST
    public Response criar(@NotNull @Valid ProdutoRequestDto request, @Context UriInfo uriInfo) {
        ProdutoBo criar = service.criar(request.nome(), request.preco(), request.quantidade());
        ProdutoResponseDto criado = ProdutoResponseDto.from(criar);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(criado.id().toString()).build())
                .entity(criado).build();
    }

    @PUT
    @Path("/{id}")
    public Response atualizar(@PathParam("id") Long id, @NotNull @Valid ProdutoRequestDto request) {
        var atualizado = service.atualizar(id, request.nome(), request.preco(), request.quantidade());
        return Response.ok(ProdutoResponseDto.from(atualizado)).build();
    }

    /** Novo caso de uso: retirar unidades, sem permitir estoque negativo. */
    @POST
    @Path("/{id}/retiradas")
    public ProdutoResponseDto retirar(@PathParam("id") Long id,
                                      @NotNull @Valid RetiradaEstoqueRequestDto request) {
        return ProdutoResponseDto.from(service.retirarEstoque(id, request.quantidade()));
    }

    @DELETE
    @Path("/{id}")
    public Response excluir(@PathParam("id") Long id) {
        service.excluir(id);
        return Response.noContent().build();
    }
}
