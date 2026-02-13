# API de Auditoria de Produtos - Spring Boot

Conversão do script Go para Spring Boot. Esta API recebe imagens de produtos e dados via REST, e utiliza o Gemini AI para auditar se o produto está conforme o esperado.

## 🚀 Tecnologias

- Java 21
- Spring Boot 3.4.1
- Google Gemini AI (gemini-1.5-flash)
- Maven
- Lombok

## 📋 Pré-requisitos

- Java 21 ou superior
- Maven 3.9+
- Chave de API do Google Gemini ([obtenha aqui](https://aistudio.google.com/))

## ⚙️ Configuração

1. **Clone o repositório** (ou use este diretório)

2. **Configure a API Key do Gemini**:

   Edite o arquivo `src/main/resources/application.properties` e substitua `SUA_CHAVE_AQUI` pela sua chave:
   
   ```properties
   gemini.api.key=SUA_CHAVE_AQUI
   ```

   Ou defina a variável de ambiente:
   
   ```bash
   export GEMINI_API_KEY=sua_chave_aqui
   ```

3. **Compile o projeto**:

   ```bash
   mvn clean package
   ```

## 🏃 Como Executar

```bash
mvn spring-boot:run
```

A API estará disponível em: `http://localhost:8080/api`

## 📡 Endpoints

### 1. Health Check

```http
GET /api/v1/auditoria/health
```

**Resposta**:
```
API de Auditoria está funcionando!
```

### 2. Auditar Produto

```http
POST /api/v1/auditoria/produtos
Content-Type: multipart/form-data
```

**Parâmetros**:
- `imagens` (file[]): Array de imagens do produto (JPG/PNG, máx 10MB cada)
  - Pode enviar quantas imagens forem necessárias (frente, verso, laterais, código de barras, etc.)
  - Todas as imagens serão analisadas pelo Gemini AI
- `dados` (string): JSON com os dados do produto

**Exemplo de JSON (dados)**:
```json
{
  "IdSolicitacao": 30470331,
  "DescricaoProduto": "Xeque mate energetico",
  "codigosDeBarras": [
    {
      "codigoBarras": "121313"
    }
  ]
}
```

**Resposta de Sucesso** (200 OK):
```json
{
  "idSolicitacao": 30470331,
  "resultado": {
    "status": "APROVADO",
    "motivo": "Código de barras confere e descrição do produto está correta"
  },
  "dataAuditoria": "2026-02-13T10:30:00",
  "mensagem": "Auditoria processada com sucesso"
}
```

**Resposta de Reprovação**:
```json
{
  "idSolicitacao": 30470331,
  "resultado": {
    "status": "REPROVADO",
    "motivo": "Código de barras na imagem (789456) não corresponde ao código do JSON (121313)"
  },
  "dataAuditoria": "2026-02-13T10:30:00",
  "mensagem": "Auditoria processada com sucesso"
}
```

**Resposta em Caso de Erro** (também REPROVADO por segurança):
```json
{
  "idSolicitacao": 30470331,
  "resultado": {
    "status": "REPROVADO",
    "motivo": "Auditoria reprovada por erro no processamento: [detalhes do erro]"
  },
  "dataAuditoria": "2026-02-13T10:30:00",
  "mensagem": "Auditoria processada com sucesso"
}
```

### ⚠️ Política de Reprovação Automática

O sistema **REPROVA automaticamente** em qualquer uma das seguintes situações:
- ❌ Erro ao processar a auditoria (conexão, timeout, etc.)
- ❌ Resposta da IA em formato inesperado ou inválido
- ❌ Status indeterminado ou diferente de "APROVADO"/"REPROVADO"
- ❌ Impossibilidade de ler ou processar as imagens
- ❌ Qualquer exceção durante o processamento

**Regra de ouro**: Em caso de dúvida, **REPROVA**! 🛡️

### ✅ Regras de Validação Inteligente

#### 1. **Código de Barras** (Validação Estrita)
- ✅ Deve ser **EXATAMENTE** idêntico ao do JSON
- ❌ Todos os dígitos devem corresponder perfeitamente
- ❌ Qualquer divergência = REPROVADO

#### 2. **Descrição do Produto** (Validação Semântica)
O sistema **ignora diferenças de formatação** que não alteram o significado:
- ✅ **Capitalização**: `Xequemate` = `XEQUE MATE` = `xeque mate`
- ✅ **Espaçamento**: `Xequemate` = `Xeque Mate` = `Xeque  Mate`
- ✅ **Acentuação similar**: `energetico` = `energético`
- ❌ **Produto diferente**: `Coca Cola` ≠ `Pepsi` = REPROVADO

**Exemplos válidos**:
- JSON: `"Xequemate energetico"` → Rótulo: `"XEQUE MATE ENERGÉTICO"` ✅ APROVADO
- JSON: `"Red Bull"` → Rótulo: `"redbull"` ✅ APROVADO
- JSON: `"Coca Cola"` → Rótulo: `"COCA-COLA"` ✅ APROVADO

**Exemplos inválidos**:
- JSON: `"Coca Cola"` → Rótulo: `"Pepsi"` ❌ REPROVADO
- JSON: `"Red Bull"` → Rótulo: `"Monster"` ❌ REPROVADO

#### 3. **Qualidade das Imagens**
- ❌ Imagens borradas, ilegíveis ou código de barras não visível = REPROVADO
- ❌ Impossibilidade de identificar o produto = REPROVADO

## 🧪 Testando com cURL

### Com 2 imagens:
```bash
curl -X POST http://localhost:8080/api/v1/auditoria/produtos \
  -F "imagens=@XEQUEMATE.jpg" \
  -F "imagens=@XEQUEMATECB.jpg" \
  -F 'dados={"IdSolicitacao":30470331,"DescricaoProduto":"Xeque mate energetico","codigosDeBarras":[{"codigoBarras":"121313"}]}'
```

### Com múltiplas imagens:
```bash
curl -X POST http://localhost:8080/api/v1/auditoria/produtos \
  -F "imagens=@frente.jpg" \
  -F "imagens=@verso.jpg" \
  -F "imagens=@lateral1.jpg" \
  -F "imagens=@lateral2.jpg" \
  -F "imagens=@codigo-barras.jpg" \
  -F 'dados={"IdSolicitacao":30470331,"DescricaoProduto":"Xeque mate energetico","codigosDeBarras":[{"codigoBarras":"121313"}]}'
```

## 🧪 Testando com Postman/Insomnia

1. Crie uma requisição POST para: `http://localhost:8080/api/v1/auditoria/produtos`
2. Selecione `multipart/form-data`
3. Adicione os campos:
   - `imagens`: selecione um ou mais arquivos de imagem (clique em "Add file" múltiplas vezes ou selecione múltiplos arquivos)
   - `dados`: cole o JSON com os dados do produto

**Importante**: No Postman/Insomnia, use o mesmo nome de campo `imagens` para todas as imagens que você adicionar.

## 📁 Estrutura do Projeto

```
src/main/java/com/raizen/auditoria/
├── AuditoriaApplication.java          # Classe principal
├── config/
│   └── WebConfig.java                 # Configuração CORS
├── controller/
│   └── AuditoriaController.java       # Endpoints REST
├── dto/
│   ├── AuditoriaRequest.java          # DTO de entrada
│   └── AuditoriaResponse.java         # DTO de saída
├── model/
│   ├── CodigoBarras.java              # Modelo de código de barras
│   ├── DadosNucleo.java               # Modelo de dados do núcleo
│   └── ResultadoAuditoria.java        # Modelo de resultado
└── service/
    ├── AuditoriaService.java          # Lógica de negócio
    └── GeminiService.java             # Integração com Gemini AI
```

## 🔧 Diferenças em relação ao script Go

1. **API REST**: Recebe dados via HTTP POST com multipart/form-data
2. **Múltiplas Imagens**: Aceita N imagens em vez de apenas 2 (frente e verso)
3. **Validação**: Validação automática de campos obrigatórios e imagens
4. **Tratamento de Erros**: Respostas HTTP apropriadas para erros
5. **Logging**: Logging detalhado para debugging
6. **CORS**: Configurado para aceitar requisições de qualquer origem
7. **Health Check**: Endpoint para verificar status da API
8. **Flexibilidade**: O Gemini AI analisa todas as imagens fornecidas automaticamente

## 🐛 Troubleshooting

### Erro de API Key inválida
- Verifique se a chave está correta em `application.properties`
- Certifique-se de que a API do Gemini está habilitada

### Erro de tamanho de arquivo
- Ajuste `spring.servlet.multipart.max-file-size` em `application.properties`

### Erro de memória
- Aumente a heap do Java: `java -Xmx2G -jar target/auditoria-produtos-1.0.0.jar`

## 📝 Licença

Este projeto é de código aberto para fins educacionais.
