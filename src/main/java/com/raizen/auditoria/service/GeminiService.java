package com.raizen.auditoria.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizen.auditoria.model.DadosNucleo;
import com.raizen.auditoria.model.ResultadoAuditoria;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GeminiService {

  @Value("${gemini.api.key}")
  private String apiKey;

  @Value("${gemini.api.url}")
  private String apiUrl;

  private final ObjectMapper objectMapper;
  private final OkHttpClient httpClient;

  public GeminiService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.httpClient = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build();
  }

  public ResultadoAuditoria auditar(byte[][] imagens, DadosNucleo dadosNucleo) {
    try {
      log.info("Iniciando auditoria para solicitação: {} com {} imagens",
          dadosNucleo.getIdSolicitacao(), imagens.length);

      // Converter imagens para Base64
      String[] imagensBase64 = new String[imagens.length];
      for (int i = 0; i < imagens.length; i++) {
        imagensBase64[i] = Base64.getEncoder().encodeToString(imagens[i]);
      }

      // Converter dados do núcleo para JSON
      String jsonNucleo = objectMapper.writeValueAsString(dadosNucleo);

      // Construir o prompt
      String prompt = construirPrompt(jsonNucleo, imagens.length);

      // Construir o payload para a API do Gemini
      String jsonPayload = construirPayload(imagensBase64, prompt);

      log.debug("Payload Gemini construído com sucesso");

      // Fazer a chamada à API do Gemini
      String resposta = chamarGeminiAPI(jsonPayload);

      // Processar a resposta
      return processarResposta(resposta);

    } catch (Exception e) {
      log.error("Erro ao auditar produto: {}", e.getMessage(), e);
      // Em caso de erro, REPROVAR automaticamente
      return new ResultadoAuditoria("REPROVADO",
          "Auditoria reprovada por erro no processamento: " + e.getMessage());
    }
  }

  private String construirPrompt(String jsonNucleo, int quantidadeImagens) {
    return "Aja como um auditor rigoroso, porém inteligente. Compare o JSON: " + jsonNucleo +
        " com as " + quantidadeImagens + " imagens fornecidas. " +
        "Analise todas as imagens do produto cuidadosamente. " +
        "\n\nREGRAS DE VALIDAÇÃO:" +
        "\n1. CÓDIGO DE BARRAS: Deve ser EXATAMENTE idêntico ao do JSON (todos os dígitos devem corresponder)." +
        "\n2. DESCRIÇÃO DO PRODUTO: " +
        "\n   - Ignore diferenças de CAPITALIZAÇÃO (maiúsculas/minúsculas)" +
        "\n   - Ignore diferenças de ESPAÇAMENTO entre palavras" +
        "\n   - Ignore acentuação se o significado for o mesmo" +
        "\n   - O CONTEÚDO SEMÂNTICO deve ser o mesmo (ex: 'Xequemate' = 'XEQUE MATE' = 'xeque mate')" +
        "\n   - REPROVE se o produto for DIFERENTE (ex: 'Coca Cola' vs 'Pepsi')" +
        "\n3. Em caso de imagens ilegíveis, borradas ou código de barras não visível, REPROVE." +
        "\n4. Em caso de QUALQUER dúvida sobre a identidade do produto, REPROVE." +
        "\n\nRetorne APENAS um JSON no formato: " +
        "{\"status\": \"APROVADO\"|\"REPROVADO\", \"motivo\": \"texto explicativo detalhado\"}";
  }

  private String construirPayload(String[] imagensBase64, String prompt) throws IOException {
    StringBuilder partsBuilder = new StringBuilder();

    // Adicionar todas as imagens
    for (String imagemBase64 : imagensBase64) {
      if (partsBuilder.length() > 0) {
        partsBuilder.append(",\n              ");
      }
      partsBuilder.append(String.format("""
          {
            "inline_data": {
              "mime_type": "image/jpeg",
              "data": "%s"
            }
          }""", imagemBase64));
    }

    // Adicionar o prompt de texto
    partsBuilder.append(",\n              ");
    partsBuilder.append(String.format("""
        {
          "text": "%s"
        }""", prompt.replace("\"", "\\\"")));

    String payload = String.format("""
        {
          "contents": [{
            "parts": [
              %s
            ]
          }],
          "generationConfig": {
            "temperature": 0.1,
            "topK": 32,
            "topP": 1,
            "maxOutputTokens": 2048
          }
        }
        """, partsBuilder.toString());

    return payload;
  }

  private String chamarGeminiAPI(String jsonPayload) throws IOException {
    String url = apiUrl + "?key=" + apiKey;

    log.debug("URL da API Gemini: {}", apiUrl);
    log.debug("Tamanho do payload: {} bytes", jsonPayload.length());

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json"));

    Request request = new Request.Builder()
        .url(url)
        .post(body)
        .addHeader("Content-Type", "application/json")
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        String errorBody = response.body() != null ? response.body().string() : "Sem detalhes";
        log.error("Erro na API Gemini: {} - {}", response.code(), errorBody);
        log.error("URL chamada: {}", apiUrl);
        throw new IOException("Erro ao chamar API Gemini: " + response.code() + " - " + errorBody);
      }

      String responseBody = response.body() != null ? response.body().string() : "";
      log.debug("Resposta da API Gemini recebida com sucesso");
      return responseBody;
    }
  }

  private ResultadoAuditoria processarResposta(String respostaJson) {
    try {
      JsonNode rootNode = objectMapper.readTree(respostaJson);

      // Navegar pela estrutura de resposta do Gemini
      JsonNode candidatesNode = rootNode.path("candidates");
      if (candidatesNode.isArray() && candidatesNode.size() > 0) {
        JsonNode firstCandidate = candidatesNode.get(0);
        JsonNode contentNode = firstCandidate.path("content");
        JsonNode partsNode = contentNode.path("parts");

        if (partsNode.isArray() && partsNode.size() > 0) {
          String textoResposta = partsNode.get(0).path("text").asText();
          log.debug("Texto da resposta Gemini: {}", textoResposta);

          // Extrair JSON da resposta (pode vir com markdown)
          String jsonLimpo = extrairJSON(textoResposta);

          // Converter para ResultadoAuditoria
          ResultadoAuditoria resultado = objectMapper.readValue(jsonLimpo, ResultadoAuditoria.class);

          // Validar o status recebido - se não for APROVADO, considerar como REPROVADO
          if (resultado.getStatus() == null ||
              (!resultado.getStatus().equalsIgnoreCase("APROVADO") &&
                  !resultado.getStatus().equalsIgnoreCase("REPROVADO"))) {
            log.warn("Status indeterminado recebido: {}. Reprovando automaticamente.", resultado.getStatus());
            return new ResultadoAuditoria("REPROVADO",
                "Status indeterminado. Motivo original: " + resultado.getMotivo());
          }

          return resultado;
        }
      }

      log.warn("Resposta do Gemini em formato inesperado - Reprovando automaticamente");
      return new ResultadoAuditoria("REPROVADO",
          "Não foi possível processar a resposta da IA. Auditoria reprovada por segurança.");

    } catch (Exception e) {
      log.error("Erro ao processar resposta do Gemini: {}", e.getMessage(), e);
      return new ResultadoAuditoria("REPROVADO",
          "Erro ao processar resposta da IA. Auditoria reprovada por segurança: " + e.getMessage());
    }
  }

  /**
   * Dado o JSON completo da solicitação (já aprovada), pede à IA para montar
   * o JSON de aprovação no formato esperado pelo sistema de cadastro de produtos.
   */
  public String montarJsonAprovacao(String jsonSolicitacaoCompleto) {
    try {
      log.info("Solicitando à IA montagem do JSON de aprovação...");

      String prompt = construirPromptJsonAprovacao(jsonSolicitacaoCompleto);

      // Payload somente texto (sem imagens)
      String jsonPayload = String.format("""
          {
            "contents": [{
              "parts": [
                { "text": %s }
              ]
            }],
            "generationConfig": {
              "temperature": 0.1,
              "topK": 32,
              "topP": 1,
              "maxOutputTokens": 4096
            }
          }
          """, objectMapper.writeValueAsString(prompt));

      String resposta = chamarGeminiAPI(jsonPayload);
      JsonNode rootNode = objectMapper.readTree(resposta);
      String textoResposta = rootNode
          .path("candidates").get(0)
          .path("content")
          .path("parts").get(0)
          .path("text").asText();

      return extrairJSON(textoResposta);

    } catch (Exception e) {
      log.error("Erro ao montar JSON de aprovação via IA: {}", e.getMessage(), e);
      throw new RuntimeException("Erro ao montar JSON de aprovação: " + e.getMessage(), e);
    }
  }

  private String construirPromptJsonAprovacao(String jsonSolicitacao) {
    return "Você é um assistente especializado em cadastro de produtos de conveniência.\n\n" +
        "Com base no JSON de solicitação abaixo, monte o JSON de aprovação do produto " +
        "no formato exato especificado.\n\n" +
        "REGRA GERAL: Quando o campo de saída tiver o mesmo nome que um campo do JSON de entrada " +
        "(em qualquer nível), copie o valor diretamente. Aplique as regras específicas " +
        "somente quando o campo tiver nome diferente ou precisar de transformação.\n\n" +
        "JSON DE ENTRADA:\n" + jsonSolicitacao + "\n\n" +
        "MAPEAMENTO DE CAMPOS (somente os que precisam de transformação ou origem específica):\n" +
        "- IdSolicitacao: copiar de IdSolicitacao (raiz)\n" +
        "- DescricaoProduto: copiar de Precadastro.DescricaoProduto\n" +
        "- Gift: copiar de Precadastro.Gift; se null usar '0'\n" +
        "- Notabilidade: copiar de Precadastro.Notabilidade; se null usar 'Não Notável'\n" +
        "- MarkUp: copiar de Precadastro.MarkUp; se null usar 0\n" +
        "- IdEstruturaMercadologica: copiar de Precadastro.IdEstruturaMercadologica (pode ser null)\n" +
        "- IdNivel1EstrMerc: copiar de Precadastro.IdNivel1EstrMerc\n" +
        "- IdNivel2EstrMerc: copiar de Precadastro.IdNivel2EstrMerc\n" +
        "- IdNivel3EstrMerc: copiar de Precadastro.IdNivel3EstrMerc (pode ser null)\n" +
        "- IdNivel4EstrMerc: null (não presente no JSON de entrada)\n" +
        "- IdSolucaoOptativa: copiar de Precadastro.IdSolucaoOptativa; se null usar 0\n" +
        "- IdMarca: copiar de Precadastro.IdMarca\n" +
        "- ConteudoEmbalagem: copiar de Precadastro.QuantidadeConteudoEmbalagem\n" +
        "- IdUnidadeMedida: copiar de Precadastro.IdUnidadeMedidaEmbalagem\n" +
        "- Segmentos: copiar de Segmentos (raiz)\n" +
        "- StatusSolicitacao: copiar de StatusSolicitacao (raiz)\n" +
        "- Observacao: copiar de Observacao (raiz)\n" +
        "- Usuario: copiar de EnviadoPor (raiz)\n" +
        "- TipoProduto: copiar de Precadastro.TipoItemMix convertendo para número; se '?' ou null usar 0\n" +
        "- Producao: '0' se não disponível\n" +
        "- codigosDeBarras: montar array a partir de listEmbalagemSolicitacao com este mapeamento:\n" +
        "    idEmbalagemSolicitacao <- IdEmbalagemSolicitacao\n" +
        "    IdSolicitacao          <- IdSolicitacao (raiz)\n" +
        "    quantidadeEmbalagem    <- QuantidadeEmbalagem\n" +
        "    idUnidadeMedida        <- IdUnidadeMedida\n" +
        "    tipoCodigoBarras       <- TipoCodigoBarras\n" +
        "    codigoBarras           <- CodigoBarras\n" +
        "    Principal              <- false\n" +
        "- Anexo: copiar NomeArquivo do primeiro item de Anexos; '' se vazio\n" +
        "- ReferenciaFabricante: '' se não disponível\n" +
        "- ForaMix: '0' se não disponível\n" +
        "- PitStop: copiar de Revendedor.PitStop\n" +
        "- Regional: '0' se não disponível\n" +
        "- DiretorioAnexo: copiar de DiretorioAnexo (raiz)\n" +
        "- DescricaoCupom: copiar de Precadastro.DescricaoProduto\n" +
        "- IdRevendedor: copiar de IdRevendedor (raiz)\n" +
        "- DataAprovacao: null\n" +
        "- AprovadoPor: null\n" +
        "- IdProduto: null\n" +
        "- idProduto: null\n\n" +
        "Retorne APENAS o JSON de saída, sem explicações, sem markdown.";
  }

  private String extrairJSON(String texto) {
    // Remove possíveis marcadores de código markdown
    texto = texto.trim();
    if (texto.startsWith("```json")) {
      texto = texto.substring(7);
    }
    if (texto.startsWith("```")) {
      texto = texto.substring(3);
    }
    if (texto.endsWith("```")) {
      texto = texto.substring(0, texto.length() - 3);
    }

    // Procura pelo JSON entre chaves
    int inicio = texto.indexOf("{");
    int fim = texto.lastIndexOf("}");

    if (inicio >= 0 && fim > inicio) {
      return texto.substring(inicio, fim + 1).trim();
    }

    return texto.trim();
  }
}
