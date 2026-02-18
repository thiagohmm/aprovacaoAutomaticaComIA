package com.raizen.auditoria.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizen.auditoria.dto.AuditoriaResponse;
import com.raizen.auditoria.model.DadosNucleo;
import com.raizen.auditoria.model.ResultadoAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

  private final GeminiService geminiService;
  private final ObjectMapper objectMapper;

  /**
   * Processa a auditoria com imagens e dados básicos do produto (DadosNucleo).
   * Usado pelo fluxo original com imagens.
   */
  public AuditoriaResponse processarAuditoria(
      MultipartFile[] imagens,
      DadosNucleo dadosNucleo) {

    try {
      log.info("Processando auditoria para solicitação: {}", dadosNucleo.getIdSolicitacao());

      if (imagens == null || imagens.length == 0) {
        throw new IllegalArgumentException("Pelo menos uma imagem é obrigatória");
      }

      for (int i = 0; i < imagens.length; i++) {
        validarImagem(imagens[i], "Imagem " + (i + 1));
      }

      byte[][] bytesImagens = new byte[imagens.length][];
      for (int i = 0; i < imagens.length; i++) {
        bytesImagens[i] = imagens[i].getBytes();
        log.debug("Imagem {} convertida - {} bytes", i + 1, bytesImagens[i].length);
      }

      ResultadoAuditoria resultado = geminiService.auditar(bytesImagens, dadosNucleo);
      log.info("Auditoria concluída - Status: {}", resultado.getStatus());

      return AuditoriaResponse.builder()
          .idSolicitacao(dadosNucleo.getIdSolicitacao())
          .resultado(resultado)
          .dataAuditoria(LocalDateTime.now())
          .mensagem("Auditoria processada com sucesso")
          .build();

    } catch (IOException e) {
      log.error("Erro ao processar imagens: {}", e.getMessage(), e);
      throw new RuntimeException("Erro ao processar imagens: " + e.getMessage(), e);
    }
  }

  /**
   * Processa a auditoria a partir do JSON completo da solicitação + imagens.
   * Se a IA aprovar, pede à IA para montar o JSON de aprovação no formato
   * correto.
   */
  public AuditoriaResponse processarAuditoriaCompleta(
      MultipartFile[] imagens,
      String jsonSolicitacaoCompleto) {

    try {
      log.info("Processando auditoria completa com JSON da solicitação");

      if (imagens == null || imagens.length == 0) {
        throw new IllegalArgumentException("Pelo menos uma imagem é obrigatória");
      }

      for (int i = 0; i < imagens.length; i++) {
        validarImagem(imagens[i], "Imagem " + (i + 1));
      }

      // Extrair DadosNucleo do JSON completo para o prompt de auditoria
      DadosNucleo dadosNucleo = extrairDadosNucleo(jsonSolicitacaoCompleto);

      byte[][] bytesImagens = new byte[imagens.length][];
      for (int i = 0; i < imagens.length; i++) {
        bytesImagens[i] = imagens[i].getBytes();
      }

      // Passo 1: IA audita as imagens vs dados do produto
      ResultadoAuditoria resultado = geminiService.auditar(bytesImagens, dadosNucleo);
      log.info("Auditoria concluída - Status: {}", resultado.getStatus());

      AuditoriaResponse.AuditoriaResponseBuilder responseBuilder = AuditoriaResponse.builder()
          .idSolicitacao(dadosNucleo.getIdSolicitacao())
          .resultado(resultado)
          .dataAuditoria(LocalDateTime.now());

      // Passo 2: Se APROVADO, pede à IA para montar o JSON de aprovação
      if ("APROVADO".equalsIgnoreCase(resultado.getStatus())) {
        log.info("Produto aprovado. Solicitando à IA montagem do JSON de aprovação...");
        String jsonAprovacaoStr = geminiService.montarJsonAprovacao(jsonSolicitacaoCompleto);
        Object jsonAprovacao = objectMapper.readValue(jsonAprovacaoStr, Object.class);
        responseBuilder
            .mensagem("Produto aprovado. JSON de aprovação montado com sucesso.")
            .jsonAprovacao(jsonAprovacao);
      } else {
        responseBuilder.mensagem("Produto reprovado: " + resultado.getMotivo());
      }

      return responseBuilder.build();

    } catch (IOException e) {
      log.error("Erro ao processar auditoria completa: {}", e.getMessage(), e);
      throw new RuntimeException("Erro ao processar auditoria: " + e.getMessage(), e);
    }
  }

  /**
   * Extrai os campos mínimos (DadosNucleo) necessários para a auditoria
   * a partir do JSON completo da solicitação.
   */
  private DadosNucleo extrairDadosNucleo(String jsonSolicitacaoCompleto) {
    try {
      com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(jsonSolicitacaoCompleto);

      Long idSolicitacao = root.path("IdSolicitacao").asLong();

      // Descrição vem do Precadastro
      String descricaoProduto = root.path("Precadastro").path("DescricaoProduto").asText("");

      // Códigos de barras vêm de listEmbalagemSolicitacao
      com.fasterxml.jackson.databind.JsonNode embalagens = root.path("listEmbalagemSolicitacao");
      java.util.List<com.raizen.auditoria.model.CodigoBarras> codigos = new java.util.ArrayList<>();
      if (embalagens.isArray()) {
        for (com.fasterxml.jackson.databind.JsonNode emb : embalagens) {
          com.raizen.auditoria.model.CodigoBarras cb = new com.raizen.auditoria.model.CodigoBarras();
          cb.setCodigoBarras(emb.path("CodigoBarras").asText());
          cb.setTipoCodigoBarras(emb.path("TipoCodigoBarras").asText());
          codigos.add(cb);
        }
      }

      return new DadosNucleo(idSolicitacao, descricaoProduto, codigos);

    } catch (Exception e) {
      log.error("Erro ao extrair DadosNucleo do JSON completo: {}", e.getMessage(), e);
      throw new RuntimeException("JSON de solicitação inválido: " + e.getMessage(), e);
    }
  }

  private void validarImagem(MultipartFile imagem, String nomeImagem) {
    if (imagem == null || imagem.isEmpty()) {
      throw new IllegalArgumentException(nomeImagem + " é obrigatória");
    }

    String contentType = imagem.getContentType();
    if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/jpg")
        && !contentType.equals("image/png"))) {
      throw new IllegalArgumentException(nomeImagem + " deve ser JPG, JPEG ou PNG");
    }

    if (imagem.getSize() > 10 * 1024 * 1024) {
      throw new IllegalArgumentException(nomeImagem + " não pode exceder 10MB");
    }
  }
}
