package studio.agendamento.agendamentos.repository;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity 
@Table(name = "agendamentos", uniqueConstraints = @UniqueConstraint(
        name = "uk_agendamento_sala_data_periodo", columnNames = {"id_sala", "data", "periodo"}))
public class AgendamentoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public Long id_sala;

    @Column(nullable = true)
    public Long id_usuario;

    @Column(nullable = false, length = 2)
    public String uf;

    @Column(nullable = false)
    public LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public PeriodoEnum periodo;
}
