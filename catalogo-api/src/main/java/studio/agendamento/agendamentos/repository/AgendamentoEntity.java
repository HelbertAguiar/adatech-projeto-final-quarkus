package studio.agendamento.agendamentos.repository;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table (name = "agendamentos")
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

    @Column(nullable = false, length = 2)
    public LocalDate data;

    @Column(nullable = false, length = 16)
    public PeriodoEnum periodo;
}
