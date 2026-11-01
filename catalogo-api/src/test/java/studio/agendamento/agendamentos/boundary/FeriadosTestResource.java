package studio.agendamento.agendamentos.boundary;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class FeriadosTestResource implements QuarkusTestResourceLifecycleManager {
    private HttpServer server;
    private ExecutorService executor;
    private final AtomicReference<String> modo = new AtomicReference<>("normal");
    private final AtomicInteger chamadas = new AtomicInteger();
    private volatile CountDownLatch barreira = new CountDownLatch(2);

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            executor = Executors.newCachedThreadPool();
            server.setExecutor(executor);
            server.createContext("/controle", exchange -> {
                modo.set(exchange.getRequestURI().getQuery());
                chamadas.set(0);
                barreira = new CountDownLatch(2);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            server.createContext("/chamadas", exchange -> {
                byte[] bytes = Integer.toString(chamadas.get()).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            });
            server.createContext("/feriados", exchange -> {
                chamadas.incrementAndGet();
                Map<String, String> query = new HashMap<>();
                for (String pair : exchange.getRequestURI().getQuery().split("&")) {
                    String[] values = pair.split("=", 2);
                    query.put(values[0], URLDecoder.decode(values[1], StandardCharsets.UTF_8));
                }
                String atual = modo.get();
                if ("concorrente".equals(atual)) {
                    barreira.countDown();
                    try {
                        if (!barreira.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("As duas reservas não chegaram ao mock.");
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                }
                boolean feriado = query.get("data").endsWith("-01-01");
                String body = "invalido".equals(atual) ? "{\"feriado\":false}"
                        : "{\"uf\":\"" + query.get("uf") + "\",\"data\":\"" + query.get("data")
                        + "\",\"feriado\":" + feriado + ",\"descricao\":\"Mock de teste\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders("falha".equals(atual) ? 503 : 200, bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            });
            server.start();
            String url = "http://127.0.0.1:" + server.getAddress().getPort();
            return Map.of("quarkus.rest-client.feriados.url", url, "mock.feriados.url", url);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public void stop() {
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
    }
}
