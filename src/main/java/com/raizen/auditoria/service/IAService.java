package com.raizen.auditoria.service;

import com.raizen.auditoria.model.DadosNucleo;
import com.raizen.auditoria.model.ResultadoAuditoria;

/**
 * Interface comum para serviços de IA (Gemini, DeepSeek, Moondream, etc.)
 */
public interface IAService {
  
  /**
   * Realiza a auditoria do produto usando IA
   * @param imagens Array de imagens em bytes
   * @param dadosNucleo Dados do produto para comparação
   * @return Resultado da auditoria (APROVADO/REPROVADO)
   */
  ResultadoAuditoria auditar(byte[][] imagens, DadosNucleo dadosNucleo);
  
  /**
   * Retorna o nome do provedor de IA
   * @return Nome do provedor (ex: "Gemini", "DeepSeek", "Moondream")
   */
  String getProviderName();
}
