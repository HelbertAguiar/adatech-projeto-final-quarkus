package studio.agendamento.agendamentos.boundary;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(value = FeriadosTestResource.class, restrictToAnnotatedClass = true)
class AgendamentoResourceTest {
    private static final LocalDate DATA = LocalDate.of(LocalDate.now().getYear() + 1, 2, 10);
    private static final LocalDate FERIADO = LocalDate.of(DATA.getYear(), 1, 1);

    @ConfigProperty(name = "mock.feriados.url")
    Optional<String> mockUrl;

    @Inject
    MeterRegistry metrics;

    @BeforeEach
    void limpar() {
        given().get("/agendamentos").then().statusCode(200).extract().jsonPath()
                .getList("id", Long.class).forEach(id ->
                        given().delete("/agendamentos/" + id).then().statusCode(204));
        modo("normal");
    }

    private void modo(String modo) {
        given().post(mockUrl.orElseThrow() + "/controle?" + modo).then().statusCode(204);
    }

    private Map<String, Object> reserva(long sala, LocalDate data, String periodo) {
        return new HashMap<>(Map.of("id_sala", sala, "id_usuario", 10,
                "uf", "SP", "data", data.toString(), "periodo", periodo));
    }

    private long criar(long sala, String periodo) {
        return given().contentType("application/json").body(reserva(sala, DATA, periodo))
                .post("/agendamentos").then().statusCode(201).extract().jsonPath().getLong("id");
    }

    private void erro(io.restassured.response.ValidatableResponse resposta, int status, String codigo) {
        resposta.statusCode(status).contentType("application/json")
                .body("codigo", equalTo(codigo), "mensagem", not(emptyOrNullString()),
                        "timestamp", not(emptyOrNullString()), "path", notNullValue(),
                        "detalhes", notNullValue());
    }

    @Test
    void crudCompletoComLocationEAtualizacaoPersistida() {
        given().get("/agendamentos").then().statusCode(200).body("size()", is(0));
        var resposta = given().contentType("application/json").body(reserva(1, DATA, "MANHA"))
                .post("/agendamentos").then().statusCode(201)
                .header("Location", matchesPattern(".*/agendamentos/[0-9]+"))
                .body("id_sala", is(1), "data", is(DATA.toString()), "periodo", is("MANHA"))
                .extract();
        long id = resposta.jsonPath().getLong("id");
        given().get(resposta.header("Location")).then().statusCode(200).body("id", is((int) id));
        given().contentType("application/json").body(reserva(2, DATA.plusDays(1), "TARDE"))
                .put("/agendamentos/" + id).then().statusCode(200);
        given().get("/agendamentos/" + id).then().statusCode(200)
                .body("id_sala", is(2), "data", is(DATA.plusDays(1).toString()), "periodo", is("TARDE"));
        given().get("/agendamentos").then().statusCode(200).body("size()", is(1));
        given().delete("/agendamentos/" + id).then().statusCode(204);
        erro(given().get("/agendamentos/" + id).then(), 404, "AGENDAMENTO_NAO_ENCONTRADO");
    }

    @Test
    void permiteUsuarioOpcionalEMantemSalaOcupada() {
        var body = reserva(1, DATA, "MANHA");
        body.remove("id_usuario");
        long id = given().contentType("application/json").body(body).post("/agendamentos")
                .then().statusCode(201).body("id_usuario", nullValue(), "salaDisponivel", is(false))
                .extract().jsonPath().getLong("id");
        given().get("/agendamentos/" + id).then().statusCode(200)
                .body("id_usuario", nullValue(), "salaDisponivel", is(false));
    }

    @Test
    void validaCamposObrigatoriosEFormatoDeErro400() {
        erro(given().contentType("application/json").body("{}").post("/agendamentos").then(),
                400, "DADOS_INVALIDOS");
    }

    @Test
    void rejeitaIdsNaoPositivosUfInvalidaEDataPassada() {
        for (var item : List.of(Map.entry("id_sala", (Object) 0), Map.entry("id_usuario", (Object) (-1)),
                Map.entry("uf", (Object) "XX"), Map.entry("uf", (Object) "sp"),
                Map.entry("data", (Object) LocalDate.now().minusDays(1).toString()))) {
            var body = reserva(1, DATA, "MANHA");
            body.put(item.getKey(), item.getValue());
            erro(given().contentType("application/json").body(body).post("/agendamentos").then(),
                    400, "DADOS_INVALIDOS");
        }
    }

    @Test
    void rejeitaJsonEnumEDataMalformados() {
        for (String body : List.of("{", "null",
                "{\"id_sala\":1,\"id_usuario\":10,\"uf\":\"SP\",\"data\":\"" + DATA + "\",\"periodo\":\"MADRUGADA\"}",
                "{\"id_sala\":1,\"id_usuario\":10,\"uf\":\"SP\",\"data\":\"amanha\",\"periodo\":\"MANHA\"}")) {
            erro(given().contentType("application/json").body(body).post("/agendamentos").then(),
                    400, "DADOS_INVALIDOS");
        }
    }

    @Test
    void padroniza404EmConsultaAtualizacaoEExclusao() {
        erro(given().get("/agendamentos/999999").then(), 404, "AGENDAMENTO_NAO_ENCONTRADO");
        erro(given().contentType("application/json").body(reserva(1, DATA, "MANHA"))
                .put("/agendamentos/999999").then(), 404, "AGENDAMENTO_NAO_ENCONTRADO");
        erro(given().delete("/agendamentos/999999").then(), 404, "AGENDAMENTO_NAO_ENCONTRADO");
    }

    @Test
    void bloqueiaConflitosMasPermiteOutrasSalasEPeriodos() {
        criar(1, "MANHA");
        erro(given().contentType("application/json").body(reserva(1, DATA, "MANHA"))
                .post("/agendamentos").then(), 409, "RESERVA_CONFLITANTE");
        var outraUf = reserva(1, DATA, "MANHA");
        outraUf.put("uf", "RJ");
        erro(given().contentType("application/json").body(outraUf)
                .post("/agendamentos").then(), 409, "RESERVA_CONFLITANTE");
        criar(1, "TARDE");
        criar(2, "MANHA");
        given().get("/agendamentos").then().body("size()", is(3));
    }

    @Test
    void putIgnoraProprioRegistroEBloqueiaReservaDeOutro() {
        long primeiro = criar(1, "MANHA");
        long segundo = criar(2, "MANHA");
        given().contentType("application/json").body(reserva(1, DATA, "MANHA"))
                .put("/agendamentos/" + primeiro).then().statusCode(200);
        erro(given().contentType("application/json").body(reserva(1, DATA, "MANHA"))
                .put("/agendamentos/" + segundo).then(), 409, "RESERVA_CONFLITANTE");
        given().get("/agendamentos/" + segundo).then().body("id_sala", is(2));
    }

    @Test
    void feriadoBloqueiaPostEPutSemModificarDados() {
        erro(given().contentType("application/json").body(reserva(1, FERIADO, "MANHA"))
                .post("/agendamentos").then(), 409, "DIA_FERIADO");
        given().get("/agendamentos").then().body("size()", is(0));
        long id = criar(1, "MANHA");
        erro(given().contentType("application/json").body(reserva(1, FERIADO, "MANHA"))
                .put("/agendamentos/" + id).then(), 409, "DIA_FERIADO");
        given().get("/agendamentos/" + id).then().body("data", is(DATA.toString()));
    }

    @Test
    void falhaExternaExecutaTresTentativasESemGravacao() {
        double antes = contador();
        modo("falha");
        erro(given().contentType("application/json").body(reserva(1, DATA, "MANHA"))
                .post("/agendamentos").then(), 503, "FERIADOS_INDISPONIVEIS");
        given().get(mockUrl.orElseThrow() + "/chamadas").then().body(equalTo("3"));
        given().get("/agendamentos").then().body("size()", is(0));
        assertEquals(antes, contador());
    }

    @Test
    void falhaExternaNoPutPreservaReserva() {
        long id = criar(1, "MANHA");
        modo("falha");
        erro(given().contentType("application/json").body(reserva(2, DATA, "TARDE"))
                .put("/agendamentos/" + id).then(), 503, "FERIADOS_INDISPONIVEIS");
        given().get("/agendamentos/" + id).then().body("id_sala", is(1), "periodo", is("MANHA"));
    }

    @Test
    void respostaExternaIncompletaTambemImpedeGravacao() {
        modo("invalido");
        erro(given().contentType("application/json").body(reserva(1, DATA, "MANHA"))
                .post("/agendamentos").then(), 503, "FERIADOS_INDISPONIVEIS");
        given().get("/agendamentos").then().body("size()", is(0));
    }

    @Test
    void contadorSomenteAumentaAposCriacaoConfirmada() {
        double antes = contador();
        long id = criar(1, "MANHA");
        assertEquals(antes + 1, contador());
        given().contentType("application/json").body(reserva(1, DATA, "MANHA"))
                .put("/agendamentos/" + id).then().statusCode(200);
        given().contentType("application/json").body(reserva(1, DATA, "MANHA"))
                .post("/agendamentos").then().statusCode(409);
        assertEquals(antes + 1, contador());
        given().get("/q/metrics").then().statusCode(200).body(containsString("agendamentos_criados_total"));
    }

    @Test
    void duasReservasSimultaneasGravamApenasUma() throws Exception {
        double antes = contador();
        modo("concorrente");
        // A barreira no mock garante que as duas transações passaram pela consulta de conflito.
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> post = () -> given().contentType("application/json")
                    .body(reserva(5, DATA, "NOITE")).post("/agendamentos").statusCode();
            var tarefas = executor.invokeAll(List.of(post, post));
            var status = new ArrayList<Integer>();
            for (var tarefa : tarefas) status.add(tarefa.get(15, TimeUnit.SECONDS));
            Collections.sort(status);
            assertEquals(List.of(201, 409), status);
        }
        given().get("/agendamentos").then().body("size()", is(1));
        assertEquals(antes + 1, contador());
    }

    @Test
    void saudeEConfiguracaoRespondem() {
        given().get("/q/health/live").then().statusCode(200).body("status", is("UP"));
        given().get("/q/health/ready").then().statusCode(200).body("status", is("UP"));
        given().get("/info").then().statusCode(200).body("nome", not(emptyOrNullString()));
    }

    private double contador() {
        return metrics.get("agendamentos.criados").counter().count();
    }
}
