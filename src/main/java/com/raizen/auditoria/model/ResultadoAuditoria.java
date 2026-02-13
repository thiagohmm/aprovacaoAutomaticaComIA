package com.raizen.auditoria.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoAuditoria {

  @JsonProperty("status")
  private String status;

  @JsonProperty("motivo")
  private String motivo;
}
