package studio.agendamento.integracao.feriados;

import java.time.LocalDate;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class FeriadosService {
    private final FeriadosClient client;

    public FeriadosService(@RestClient FeriadosClient client) {
        this.client = client;
    }

    @Retry(maxRetries = 2, delay = 100, jitter = 0)
    @Fallback(fallbackMethod = "indisponivel")
    public boolean ehFeriado(String uf, LocalDate data) {
        FeriadoResponse resposta = client.consultar(uf, data);
        if (resposta == null || resposta.feriado() == null
                || !uf.equals(resposta.uf()) || !data.equals(resposta.data())) {
            throw new IllegalStateException("Resposta inválida do serviço de feriados.");
        }
        return resposta.feriado();
    }

    public boolean indisponivel(String uf, LocalDate data) {
        throw new FeriadosIndisponiveisException();
    }
}
