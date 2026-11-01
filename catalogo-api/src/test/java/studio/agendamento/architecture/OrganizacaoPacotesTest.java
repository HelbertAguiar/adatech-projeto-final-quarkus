package studio.agendamento.architecture;

import java.time.LocalDate;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import studio.agendamento.agendamentos.boundary.*;
import studio.agendamento.agendamentos.domain.*;
import studio.agendamento.agendamentos.repository.*;
import static org.junit.jupiter.api.Assertions.*;

class OrganizacaoPacotesTest {
    @Test
    void mantemEntityDtoEBoSeparados() throws Exception {
        assertEquals("studio.agendamento.agendamentos.boundary", AgendamentoResource.class.getPackageName());
        assertEquals("studio.agendamento.agendamentos.domain", AgendamentoService.class.getPackageName());
        assertEquals("studio.agendamento.agendamentos.repository", AgendamentoRepository.class.getPackageName());
        assertNotNull(AgendamentoEntity.class.getAnnotation(Entity.class));
        assertNull(AgendamentoBo.class.getAnnotation(Entity.class));
        assertNull(AgendamentoRequestDto.class.getAnnotation(Entity.class));
        assertEquals(AgendamentoBo.class, AgendamentoService.class.getMethod("criar", Long.class,
                Long.class, String.class, LocalDate.class, PeriodoEnum.class).getReturnType());
        for (var method : AgendamentoService.class.getDeclaredMethods()) {
            for (var parameter : method.getParameterTypes()) {
                assertFalse(parameter.getPackageName().contains(".boundary"));
            }
            assertFalse(method.getReturnType().getPackageName().contains(".boundary"));
        }
    }
}
