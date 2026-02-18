package com.raizen.auditoria.dto;

import com.raizen.auditoria.model.ResultadoAuditoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaResponse {

  private Long idSolicitacao;
  private ResultadoAuditoria resultado;
  private LocalDateTime dataAuditoria;
  private String mensagem;
  // JSON montado pela IA para envio ao sistema, preenchido apenas quando APROVADO
  private Object jsonAprovacao;
}
