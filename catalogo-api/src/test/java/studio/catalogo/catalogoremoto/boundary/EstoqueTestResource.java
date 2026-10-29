package studio.catalogo.catalogoremoto.boundary;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Simula o serviço remoto durante os testes, sem dependência de processos externos. */
public class EstoqueTestResource implements QuarkusTestResourceLifecycleManager {

    private static final AtomicBoolean indisponivel = new AtomicBoolean();
    private HttpServer server;

    public static void setIndisponivel(boolean valor) {
        indisponivel.set(valor);
    }

    @Override
    public Map<String, String> start() {
        try {
            indisponivel.set(false);
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/products", exchange -> {
                try (exchange) {
                    boolean falha = indisponivel.get();
                    int status = falha ? 503 : 200;
                    String resposta = falha
                            ? "{\"error\":\"estoque temporariamente indisponivel\"}"
                            : "{\"products\":[{\"id\":1,\"title\":\"Caderno\",\"stock\":5},"
                              + "{\"id\":2,\"title\":\"Caneta\",\"stock\":1},"
                              + "{\"id\":3,\"title\":\"Pasta\",\"stock\":0}],\"total\":3}";
                    byte[] body = resposta.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, body.length);
                    exchange.getResponseBody().write(body);
                }
            });
            server.start();
            return Map.of("quarkus.rest-client.estoque-local.url",
                    "http://127.0.0.1:" + server.getAddress().getPort());
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível iniciar o estoque de testes", e);
        }
    }

    @Override
    public void stop() {
        indisponivel.set(false);
        if (server != null) {
            server.stop(0);
        }
    }
}
