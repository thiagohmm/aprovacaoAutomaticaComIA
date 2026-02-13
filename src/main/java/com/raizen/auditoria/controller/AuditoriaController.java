package com.raizen.auditoria.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizen.auditoria.dto.AuditoriaResponse;
import com.raizen.auditoria.model.DadosNucleo;
import com.raizen.auditoria.service.AuditoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

  private final AuditoriaService auditoriaService;
  private final ObjectMapper objectMapper;

  @PostMapping(value = "/produtos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<AuditoriaResponse> auditarProduto(
      @RequestParam("imagens") MultipartFile[] imagens,
      @RequestParam("dados") String dadosJson) {

    try {
      log.info("Recebida requisição de auditoria com {} imagens", imagens != null ? imagens.length : 0);
      log.debug("Dados recebidos: {}", dadosJson);

      // Converter JSON string para objeto
      DadosNucleo dadosNucleo = objectMapper.readValue(dadosJson, DadosNucleo.class);

      // Processar auditoria
      AuditoriaResponse response = auditoriaService.processarAuditoria(
          imagens,
          dadosNucleo);

      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.error("Erro de validação: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(AuditoriaResponse.builder()
              .mensagem("Erro de validação: " + e.getMessage())
              .build());

    } catch (Exception e) {
      log.error("Erro ao processar auditoria: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(AuditoriaResponse.builder()
              .mensagem("Erro interno ao processar auditoria: " + e.getMessage())
              .build());
    }
  }

  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("API de Auditoria está funcionando!");
  }
}
