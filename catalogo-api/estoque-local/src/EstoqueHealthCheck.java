import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Health check HTTP do container de estoque (sem instalar curl/wget na imagem).
 * Apenas devolve um exit code para o Docker, sem alterar os endpoints da aula.
 */
public final class EstoqueHealthCheck {
    public static void main(String[] args) {
        try (var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8081/products"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            System.exit(status == 200 ? 0 : 1);
        } catch (Exception exception) {
            System.err.println("Estoque indisponivel: " + exception.getMessage());
            System.exit(1);
        }
    }
}
