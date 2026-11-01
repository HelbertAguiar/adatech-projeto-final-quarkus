package studio.agendamento.agendamentos.boundary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.RequiredArgsConstructor;
import studio.agendamento.agendamentos.domain.AgendamentoService;

@Path("/agendamentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class AgendamentoResource {
    private final AgendamentoService service;

    @GET
    public Response listar() {
        return Response.ok(service.listar().stream().map(AgendamentoResponseDto::from).toList()).build();
    }

    @GET
    @Path("/{id}")
    public AgendamentoResponseDto buscar(@PathParam("id") @Positive Long id) {
        return AgendamentoResponseDto.from(service.buscar(id));
    }

    @POST
    public Response criar(@NotNull @Valid AgendamentoRequestDto request, @Context UriInfo uriInfo) {
        var bo = service.criar(request.id_sala(), request.id_usuario(), request.uf(), request.data(), request.periodo());
        var resposta = AgendamentoResponseDto.from(bo);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(bo.id().toString()).build())
                .entity(resposta).build();
    }

    @PUT
    @Path("/{id}")
    public AgendamentoResponseDto atualizar(@PathParam("id") @Positive Long id,
                                           @NotNull @Valid AgendamentoRequestDto request) {
        return AgendamentoResponseDto.from(service.atualizar(id, request.id_sala(),
                request.id_usuario(), request.uf(), request.data(), request.periodo()));
    }

    @DELETE
    @Path("/{id}")
    public Response excluir(@PathParam("id") @Positive Long id) {
        service.excluir(id);
        return Response.noContent().build();
    }
}
