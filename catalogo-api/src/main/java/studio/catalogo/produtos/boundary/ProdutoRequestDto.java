package studio.catalogo.produtos.boundary;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Payload de POST/PUT, separado da entidade (nao recebe ID do cliente). */
public record ProdutoRequestDto(
        @NotBlank(message = "Nome é obrigatório") @Size(max = 120) String nome,
        @NotNull @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 8, fraction = 2) BigDecimal preco,
        @NotNull @Min(0) Integer quantidade
) {
}
