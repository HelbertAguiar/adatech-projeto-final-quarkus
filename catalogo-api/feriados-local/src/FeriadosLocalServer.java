import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;

/** Mock didático, não é um calendário oficial. Executar com Java 21. */
public class FeriadosLocalServer {
    public static void main(String[] args) throws Exception {
        String host = System.getenv().getOrDefault("FERIADOS_HOST", "127.0.0.1");
        int port = Integer.parseInt(System.getenv().getOrDefault("FERIADOS_PORT", "8081"));
        // Valores adicionais: SP:2030-04-10,RJ:2030-05-20
        Set<String> extras = new HashSet<>(Arrays.asList(
                System.getenv().getOrDefault("FERIADOS_EXTRAS", "").split(",")));
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        var executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/feriados", exchange -> {
            int status = 200;
            String body;
            try {
                if (!"GET".equals(exchange.getRequestMethod()) || !"/feriados".equals(exchange.getRequestURI().getPath())) {
                    status = 405;
                    body = "{\"erro\":\"Use GET /feriados\"}";
                } else {
                    Map<String, String> query = new HashMap<>();
                    String raw = exchange.getRequestURI().getRawQuery();
                    if (raw != null) {
                        for (String pair : raw.split("&")) {
                            String[] values = pair.split("=", 2);
                            if (values.length == 2) {
                                query.put(URLDecoder.decode(values[0], StandardCharsets.UTF_8),
                                        URLDecoder.decode(values[1], StandardCharsets.UTF_8));
                            }
                        }
                    }
                    String uf = query.get("uf");
                    if (uf == null || !uf.matches("AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO")) {
                        throw new IllegalArgumentException("UF inválida");
                    }
                    LocalDate data = LocalDate.parse(query.getOrDefault("data", ""));
                    boolean feriado = (data.getMonthValue() == 1 && data.getDayOfMonth() == 1)
                            || extras.contains(uf + ":" + data);
                    body = "{\"uf\":\"" + uf + "\",\"data\":\"" + data
                            + "\",\"feriado\":" + feriado + ",\"descricao\":\""
                            + (feriado ? "Feriado simulado" : "Dia comum simulado") + "\"}";
                }
            } catch (IllegalArgumentException | java.time.format.DateTimeParseException exception) {
                status = 400;
                body = "{\"erro\":\"Informe uf e data válidas\"}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            executor.shutdownNow();
        }));
        server.start();
        System.out.println("Mock de feriados em http://" + host + ":" + port + "/feriados");
    }
}
