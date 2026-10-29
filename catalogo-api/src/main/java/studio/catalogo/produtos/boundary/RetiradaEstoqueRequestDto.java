package studio.catalogo.produtos.boundary;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Aula 06: o corpo de uma retirada deve conter uma quantidade inteira positiva. */
public record RetiradaEstoqueRequestDto(
        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que zero") Integer quantidade
) {
}
