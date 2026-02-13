package com.raizen.auditoria.service;

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

  public AuditoriaResponse processarAuditoria(
      MultipartFile[] imagens,
      DadosNucleo dadosNucleo) {

    try {
      log.info("Processando auditoria para solicitação: {}", dadosNucleo.getIdSolicitacao());

      // Validar que há pelo menos uma imagem
      if (imagens == null || imagens.length == 0) {
        throw new IllegalArgumentException("Pelo menos uma imagem é obrigatória");
      }

      // Validar todas as imagens
      for (int i = 0; i < imagens.length; i++) {
        validarImagem(imagens[i], "Imagem " + (i + 1));
      }

      // Converter imagens para bytes
      byte[][] bytesImagens = new byte[imagens.length][];
      for (int i = 0; i < imagens.length; i++) {
        bytesImagens[i] = imagens[i].getBytes();
        log.debug("Imagem {} convertida - {} bytes", i + 1, bytesImagens[i].length);
      }

      // Chamar o serviço Gemini
      ResultadoAuditoria resultado = geminiService.auditar(bytesImagens, dadosNucleo);

      log.info("Auditoria concluída - Status: {}", resultado.getStatus());

      // Construir resposta
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

  private void validarImagem(MultipartFile imagem, String nomeImagem) {
    if (imagem == null || imagem.isEmpty()) {
      throw new IllegalArgumentException(nomeImagem + " é obrigatória");
    }

    String contentType = imagem.getContentType();
    if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/jpg")
        && !contentType.equals("image/png"))) {
      throw new IllegalArgumentException(nomeImagem + " deve ser JPG, JPEG ou PNG");
    }

    // Validar tamanho (10MB)
    if (imagem.getSize() > 10 * 1024 * 1024) {
      throw new IllegalArgumentException(nomeImagem + " não pode exceder 10MB");
    }
  }
}
