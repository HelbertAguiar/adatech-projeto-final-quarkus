package studio.agendamento.agendamentos.repository;

public enum PeriodoEnum {
    
    MANHA("MANHA"),
    TARDE("TARDE"),
    NOITE("NOITE");

    private final String codigo;

    PeriodoEnum(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
