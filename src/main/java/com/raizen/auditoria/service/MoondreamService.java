package com.raizen.auditoria.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizen.auditoria.model.DadosNucleo;
import com.raizen.auditoria.model.ResultadoAuditoria;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Serviço Moondream para auditoria de produtos (OFFLINE).
 * 
 * Moondream é um modelo de visão leve e rápido que roda localmente via Ollama.
 * Ideal para ambientes sem internet ou quando privacidade é crítica.
 * 
 * NOTA: Moondream é menos preciso que Gemini, mas roda totalmente offline.
 */
@Slf4j
@Service("moondreamService")
@ConditionalOnProperty(name = "ai.provider", havingValue = "moondream")
public class MoondreamService implements IAService {

  @Value("${moondream.api.url:http://localhost:11434}")
  private String apiUrl;

  @Value("${moondream.model:moondream}")
  private String model;

  private final ObjectMapper objectMapper;
  private final OkHttpClient httpClient;

  public MoondreamService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.httpClient = new OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)  // Moondream pode ser mais lento
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build();
  }

  @Override
  public ResultadoAuditoria auditar(byte[][] imagens, DadosNucleo dadosNucleo) {
    try {
      log.info("[Moondream] 🌙 Iniciando auditoria OFFLINE para solicitação: {} com {} imagens",
          dadosNucleo.getIdSolicitacao(), imagens.length);

      // Converter imagens para Base64
      String[] imagensBase64 = new String[imagens.length];
      for (int i = 0; i < imagens.length; i++) {
        imagensBase64[i] = Base64.getEncoder().encodeToString(imagens[i]);
      }

      // Extrair código de barras esperado
      String codigoBarrasEsperado = dadosNucleo.getCodigosDeBarras().get(0).getCodigoBarras();
      String descricaoEsperada = dadosNucleo.getDescricaoProduto();

      // Processar cada imagem separadamente
      List<String> textosExtraidos = new ArrayList<>();
      
      for (int i = 0; i < imagensBase64.length; i++) {
        log.info("[Moondream] 📸 Analisando imagem {} de {}", i + 1, imagensBase64.length);
        
        // Construir prompt para extrair informações
        String prompt = construirPromptExtracao(i + 1, imagensBase64.length);
        
        // Construir payload com uma imagem
        String jsonPayload = construirPayloadUmaImagem(imagensBase64[i], prompt);
        
        // Chamar API
        String resposta = chamarMoondreamAPI(jsonPayload);
        
        // Extrair texto da resposta
        String textoExtraido = extrairTextoResposta(resposta);
        textosExtraidos.add(textoExtraido);
        
        log.info("[Moondream] 📝 Texto extraído da imagem {}: {}", i + 1, textoExtraido);
      }

      // Validar os dados extraídos contra os esperados
      return validarDadosExtraidos(textosExtraidos, codigoBarrasEsperado, descricaoEsperada);

    } catch (Exception e) {
      log.error("[Moondream] Erro ao auditar produto: {}", e.getMessage(), e);
      return new ResultadoAuditoria("REPROVADO",
          "Auditoria reprovada por erro no processamento offline: " + e.getMessage());
    }
  }

  private String construirPromptExtracao(int numeroImagem, int totalImagens) {
    return "Read all text visible on this product label. " +
           "List: 1) All numbers you see (especially barcodes) 2) Product name and description. " +
           "Be specific and accurate.";
  }

  private String extrairTextoResposta(String respostaJson) throws IOException {
    JsonNode root = objectMapper.readTree(respostaJson);
    return root.get("response").asText();
  }

  private ResultadoAuditoria validarDadosExtraidos(List<String> textosExtraidos, 
      String codigoBarrasEsperado, String descricaoEsperada) {
    
    StringBuilder detalhesAnalise = new StringBuilder();
    detalhesAnalise.append("🔍 ANÁLISE MOONDREAM (Offline)\n\n");
    
    // Juntar todos os textos extraídos
    String textoCompleto = String.join(" ", textosExtraidos).toUpperCase();
    
    boolean codigoBarrasEncontrado = false;
    boolean descricaoEncontrada = false;
    
    // Verificar código de barras
    detalhesAnalise.append("📊 CÓDIGO DE BARRAS:\n");
    detalhesAnalise.append(String.format("   Esperado: %s\n", codigoBarrasEsperado));
    
    // Procurar o código de barras (pode estar sem alguns dígitos ou com espaços)
    String codigoLimpo = codigoBarrasEsperado.replaceAll("[^0-9]", "");
    if (textoCompleto.contains(codigoLimpo)) {
      codigoBarrasEncontrado = true;
      detalhesAnalise.append("   ✅ ENCONTRADO nas imagens\n\n");
    } else {
      // Tentar match parcial (pelo menos 80% dos dígitos)
      int digitos = codigoLimpo.length();
      String subCodigo = codigoLimpo.substring(0, Math.max(1, (int)(digitos * 0.8)));
      if (textoCompleto.contains(subCodigo)) {
        codigoBarrasEncontrado = true;
        detalhesAnalise.append("   ⚠️  PARCIALMENTE encontrado (conferir manualmente)\n\n");
      } else {
        detalhesAnalise.append("   ❌ NÃO ENCONTRADO nas imagens\n\n");
      }
    }
    
    // Verificar descrição do produto
    detalhesAnalise.append("📦 DESCRIÇÃO DO PRODUTO:\n");
    detalhesAnalise.append(String.format("   Esperado: %s\n", descricaoEsperada));
    
    // Quebrar descrição em palavras-chave principais
    String[] palavrasChave = descricaoEsperada.toUpperCase()
        .replaceAll("[^A-Z0-9\\s]", " ")
        .split("\\s+");
    
    int palavrasEncontradas = 0;
    for (String palavra : palavrasChave) {
      if (palavra.length() >= 3 && textoCompleto.contains(palavra)) {
        palavrasEncontradas++;
      }
    }
    
    double percentualMatch = palavrasChave.length > 0 
        ? (palavrasEncontradas * 100.0 / palavrasChave.length) 
        : 0;
    
    if (percentualMatch >= 70) {
      descricaoEncontrada = true;
      detalhesAnalise.append(String.format("   ✅ ENCONTRADO (%.0f%% das palavras)\n\n", percentualMatch));
    } else if (percentualMatch >= 40) {
      descricaoEncontrada = true;
      detalhesAnalise.append(String.format("   ⚠️  PARCIALMENTE encontrado (%.0f%% - conferir manualmente)\n\n", percentualMatch));
    } else {
      detalhesAnalise.append(String.format("   ❌ NÃO ENCONTRADO (apenas %.0f%% das palavras)\n\n", percentualMatch));
    }
    
    // Mostrar o que foi extraído
    detalhesAnalise.append("📄 TEXTOS EXTRAÍDOS DAS IMAGENS:\n");
    for (int i = 0; i < textosExtraidos.size(); i++) {
      detalhesAnalise.append(String.format("   Imagem %d: %s\n", i + 1, textosExtraidos.get(i)));
    }
    
    // Decisão final
    if (codigoBarrasEncontrado && descricaoEncontrada) {
      return new ResultadoAuditoria("APROVADO", 
          "✅ APROVADO - Código de barras e descrição encontrados!\n\n" + detalhesAnalise.toString());
    } else {
      String motivo = !codigoBarrasEncontrado && !descricaoEncontrada
          ? "Código de barras E descrição não encontrados"
          : !codigoBarrasEncontrado 
              ? "Código de barras não encontrado"
              : "Descrição do produto não encontrada";
      
      return new ResultadoAuditoria("REPROVADO",
          String.format("❌ REPROVADO - %s\n\n%s", motivo, detalhesAnalise.toString()));
    }
  }

  private ResultadoAuditoria consolidarResultados(List<ResultadoAuditoria> resultados, int totalImagens) {
    // Se qualquer imagem reprovar, reprovar tudo
    long reprovadas = resultados.stream()
        .filter(r -> "REPROVADO".equals(r.getStatus()))
        .count();
    
    if (reprovadas > 0) {
      StringBuilder motivo = new StringBuilder();
      motivo.append(String.format("Auditoria REPROVADA. %d de %d imagens reprovadas.\n\n", 
          reprovadas, totalImagens));
      
      for (int i = 0; i < resultados.size(); i++) {
        ResultadoAuditoria r = resultados.get(i);
        motivo.append(String.format("Imagem %d: %s - %s\n", 
            i + 1, r.getStatus(), r.getMotivo()));
      }
      
      return new ResultadoAuditoria("REPROVADO", motivo.toString());
    }
    
    // Todas aprovadas
    StringBuilder motivo = new StringBuilder();
    motivo.append(String.format("✅ Todas as %d imagens foram APROVADAS.\n\n", totalImagens));
    
    for (int i = 0; i < resultados.size(); i++) {
      ResultadoAuditoria r = resultados.get(i);
      motivo.append(String.format("Imagem %d: %s\n", i + 1, r.getMotivo()));
    }
    
    return new ResultadoAuditoria("APROVADO", motivo.toString());
  }

  @Override
  public String getProviderName() {
    return "Moondream (Offline)";
  }

  private String construirPromptParaImagem(String jsonNucleo, int numeroImagem, int totalImagens) {
    // Moondream funciona melhor com perguntas diretas e simples
    return "What text do you see on this product label? List all numbers and words you can read.";
  }

  private String construirPayloadUmaImagem(String imagemBase64, String prompt) {
    return String.format("""
        {
          "model": "%s",
          "prompt": "%s",
          "images": ["%s"],
          "stream": false,
          "options": {
            "temperature": 0.1,
            "num_predict": 200,
            "stop": ["}"]
          }
        }
        """, model, escapeJson(prompt), imagemBase64);
  }

  private String escapeJson(String str) {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private String chamarMoondreamAPI(String jsonPayload) throws IOException {
    MediaType JSON = MediaType.get("application/json; charset=utf-8");
    RequestBody body = RequestBody.create(jsonPayload, JSON);

    String endpoint = apiUrl + "/api/generate";
    
    Request request = new Request.Builder()
        .url(endpoint)
        .addHeader("Content-Type", "application/json")
        .post(body)
        .build();

    log.info("[Moondream] 🌙 Enviando requisição para Ollama: {}", endpoint);

    try (Response response = httpClient.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";

      if (!response.isSuccessful()) {
        log.error("[Moondream] Erro na API Ollama. Status: {}, Body: {}", response.code(), responseBody);
        
        if (response.code() == 404) {
          throw new IOException("Modelo Moondream não encontrado. Execute: docker exec moondream ollama pull moondream");
        }
        
        throw new IOException("Erro na API Ollama/Moondream: " + response.code() + " - " + responseBody);
      }

      log.info("[Moondream] ✅ Resposta recebida com sucesso");
      log.debug("[Moondream] Response body: {}", responseBody);

      return responseBody;
    }
  }

  private ResultadoAuditoria processarResposta(String respostaJson) {
    try {
      JsonNode root = objectMapper.readTree(respostaJson);
      
      // Ollama retorna: {"response": "texto da resposta"}
      String conteudo = root.get("response").asText();
      log.debug("[Moondream] Conteúdo da resposta: {}", conteudo);

      // Tentar extrair JSON do conteúdo
      String jsonLimpo = extrairJSON(conteudo);
      if (jsonLimpo == null || jsonLimpo.isEmpty()) {
        log.warn("[Moondream] Não encontrou JSON válido na resposta. Usando análise de texto.");
        return analisarTextoLivre(conteudo);
      }

      JsonNode resultadoNode = objectMapper.readTree(jsonLimpo);

      // Verificar se tem os campos esperados
      if (resultadoNode.has("status") && resultadoNode.has("motivo")) {
        String status = resultadoNode.get("status").asText().toUpperCase();
        String motivo = resultadoNode.get("motivo").asText();
        log.info("[Moondream] 🌙 Status: {}, Motivo: {}", status, motivo);
        return new ResultadoAuditoria(status, motivo);
      }

      // Se não tem status/motivo, fazer análise textual
      log.warn("[Moondream] JSON não tem campos 'status' e 'motivo'. Usando análise de texto.");
      return analisarTextoLivre(conteudo);

    } catch (Exception e) {
      log.error("[Moondream] Erro ao processar resposta JSON: {}", e.getMessage(), e);
      
      // Tentar análise textual como fallback
      try {
        JsonNode root = objectMapper.readTree(respostaJson);
        String conteudo = root.get("response").asText();
        return analisarTextoLivre(conteudo);
      } catch (Exception ex) {
        return new ResultadoAuditoria("REPROVADO",
            "Não foi possível processar a resposta do Moondream. Erro: " + e.getMessage());
      }
    }
  }

  private ResultadoAuditoria analisarTextoLivre(String texto) {
    // Moondream retorna texto descritivo. Vamos apenas mostrar o que ele viu
    // e deixar o sistema REPROVAR por padrão (segurança)
    
    if (texto == null || texto.trim().isEmpty()) {
      return new ResultadoAuditoria("REPROVADO",
          "Moondream não retornou nenhuma análise da imagem.");
    }
    
    // Se contém palavras-chave positivas sem negativas
    String textoUpper = texto.toUpperCase();
    if (textoUpper.contains("APROVADO") && !textoUpper.contains("REPROVADO")) {
      return new ResultadoAuditoria("APROVADO", 
          "Moondream aprovou. Detalhes: " + texto);
    }
    
    // Caso contrário, mostrar o que foi detectado mas reprovar por segurança
    return new ResultadoAuditoria("REPROVADO",
        String.format("⚠️  ATENÇÃO: Moondream detectou o seguinte na imagem:\n\n%s\n\n" +
            "Reprovado por segurança. Analise manual necessária para confirmar se os dados " +
            "correspondem ao esperado.", texto.trim()));
  }

  private String extrairJSON(String texto) {
    int inicioJson = texto.indexOf("{");
    int fimJson = texto.lastIndexOf("}");

    if (inicioJson >= 0 && fimJson > inicioJson) {
      return texto.substring(inicioJson, fimJson + 1);
    }

    // Se não encontrar JSON estruturado, retornar null
    return null;
  }
}
