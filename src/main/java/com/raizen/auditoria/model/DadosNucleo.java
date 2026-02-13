package com.raizen.auditoria.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DadosNucleo {

  @NotNull(message = "IdSolicitacao é obrigatório")
  @JsonProperty("IdSolicitacao")
  private Long idSolicitacao;

  @NotBlank(message = "DescricaoProduto é obrigatória")
  @JsonProperty("DescricaoProduto")
  private String descricaoProduto;

  @JsonProperty("codigosDeBarras")
  private List<CodigoBarras> codigosDeBarras;
}
