import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Health check HTTP do mock de feriados. Deve responder 200 em /feriados.
 * Usa apenas o JDK, sem instalar curl/wget na imagem.
 */
public final class FeriadosHealthCheck {
    private FeriadosHealthCheck() {
    }

    public static void main(String[] args) {
        try (var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build()) {
            var request = HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:8081/feriados?uf=SP&data=2030-01-01"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            System.exit(status == 200 ? 0 : 1);
        } catch (Exception exception) {
            System.err.println("Feriados indisponivel: " + exception.getMessage());
            System.exit(1);
        }
    }
}
