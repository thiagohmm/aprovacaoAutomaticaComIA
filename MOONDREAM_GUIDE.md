# 🌙 Guia de Uso - Moondream (Modelo Offline)

## 📋 O que é Moondream?

Moondream é um modelo de IA leve (~2GB) com capacidade de análise de imagens que **roda totalmente offline** via Docker/Ollama.

### ✅ Vantagens:
- 🔒 **Privacidade Total** - Dados não saem da sua máquina
- 🌐 **Funciona Offline** - Não precisa de internet
- 💰 **Gratuito** - Sem custos de API
- ⚡ **Rápido** - Modelo leve e otimizado
- 🐳 **Fácil Setup** - Roda em Docker

### ⚠️ Limitações:
- 📉 **Menos preciso** que Gemini (especialmente para códigos de barras)
- 🖼️ **Uma imagem por vez** - Analisa apenas a primeira imagem
- 🐌 **Mais lento** sem GPU
- 💻 **Requer recursos locais** - RAM e CPU

---

## 🚀 Setup Rápido (3 passos)

### **Passo 1: Instalar Moondream**
```bash
./setup-moondream.sh
```

Isso vai:
- ✅ Iniciar container Ollama no Docker
- ✅ Baixar modelo Moondream (~2GB)
- ✅ Testar se está funcionando

**Tempo estimado:** 5-10 minutos (depende da internet para download)

### **Passo 2: Configurar Provedor**
```bash
# Automático via script
./switch-provider.sh moondream

# OU manual - edite application.properties
ai.provider=moondream
```

### **Passo 3: Iniciar Aplicação**
```bash
./start-with-moondream.sh
```

---

## 🔧 Setup Manual Detalhado

### **1. Requisitos:**
- ✅ Docker instalado
- ✅ Docker Compose instalado
- ✅ 4GB RAM disponível
- ✅ 5GB espaço em disco

### **2. Iniciar Ollama:**
```bash
# Método 1: Docker Compose (recomendado)
docker-compose up -d moondream

# Método 2: Docker direto
docker run -d \
  --name moondream \
  -p 11434:11434 \
  -v ollama-data:/root/.ollama \
  ollama/ollama:latest
```

### **3. Baixar Modelo:**
```bash
# Aguardar Ollama iniciar (30 segundos)
sleep 30

# Baixar Moondream
docker exec moondream ollama pull moondream
```

### **4. Testar:**
```bash
# Teste simples
docker exec moondream ollama run moondream "Descreva uma imagem de teste"

# Teste via API
curl http://localhost:11434/api/generate -d '{
  "model": "moondream",
  "prompt": "Descreva esta imagem",
  "stream": false
}'
```

### **5. Compilar Projeto:**
```bash
mvn clean package -DskipTests
```

### **6. Executar:**
```bash
java -jar target/auditoria-produtos-1.0.0.jar
```

---

## 🐳 Usando Docker Compose (Tudo em um)

```bash
# Iniciar tudo (API + Moondream)
docker-compose up -d

# Ver logs
docker-compose logs -f

# Parar tudo
docker-compose down

# Remover volumes (limpar cache)
docker-compose down -v
```

---

## 📊 Verificar Status

### **Ollama/Moondream:**
```bash
# Container rodando?
docker ps | grep moondream

# Logs
docker logs moondream

# Modelos instalados
docker exec moondream ollama list

# Testar API
curl http://localhost:11434/api/tags
```

### **Aplicação Java:**
```bash
# Health check
curl http://localhost:8080/api/v1/auditoria/health

# Ver logs
tail -f logs/application.log
```

---

## 🧪 Teste Completo

```bash
# 1. Testar com imagem do Xequemate
curl -X POST http://localhost:8080/api/v1/auditoria/produtos \
  -F "imagens=@xequemate.jpg" \
  -F 'dados={
    "IdSolicitacao": 12345,
    "DescricaoProduto": "XEQUEMATE",
    "codigosDeBarras": [{"codigoBarras": "7898357417489"}]
  }' | jq

# 2. Verificar resposta
# {
#   "idSolicitacao": 12345,
#   "resultado": {
#     "status": "APROVADO" ou "REPROVADO",
#     "motivo": "..."
#   },
#   "dataAuditoria": "...",
#   "mensagem": "Auditoria processada com sucesso"
# }
```

---

## ⚡ Comparação de Performance

| Provedor | Tempo Resposta | Precisão | Offline | Custo |
|----------|---------------|----------|---------|-------|
| **Gemini** | ~2s | ⭐⭐⭐⭐⭐ | ❌ | 🆓 |
| **Moondream** | ~5-10s | ⭐⭐⭐ | ✅ | 🆓 |

---

## 🔄 Trocar entre Provedores

```bash
# Para Gemini (online, mais preciso)
./switch-provider.sh gemini

# Para Moondream (offline, privado)
./switch-provider.sh moondream

# Verificar provedor ativo
./switch-provider.sh
```

---

## 🐛 Troubleshooting

### **Erro: "Connection refused"**
```bash
# Verificar se Ollama está rodando
docker ps | grep moondream

# Reiniciar
docker-compose restart moondream
```

### **Erro: "Model not found"**
```bash
# Baixar modelo
docker exec moondream ollama pull moondream

# Verificar modelos
docker exec moondream ollama list
```

### **Erro: "Out of memory"**
```bash
# Verificar uso de memória
docker stats moondream

# Aumentar limite (docker-compose.yml)
# mem_limit: 4g
```

### **Resposta muito lenta**
```bash
# Com GPU (NVIDIA):
# Descomente no docker-compose.yml:
# deploy:
#   resources:
#     reservations:
#       devices:
#         - driver: nvidia
#           count: 1
#           capabilities: [gpu]
```

### **Modelo responde em formato errado**
- Moondream às vezes não segue formato JSON perfeitamente
- O código tenta extrair JSON automaticamente
- Em caso de erro, produto é REPROVADO por segurança

---

## 💡 Dicas de Uso

### **Para Desenvolvimento:**
✅ Use Moondream - rápido para testar sem gastar API calls

### **Para Produção:**
✅ Use Gemini - mais preciso e confiável

### **Para Ambientes Restritos:**
✅ Use Moondream - funciona sem internet

### **Para Máxima Precisão:**
✅ Use Gemini - melhor para códigos de barras

---

## 📈 Melhorando Precisão do Moondream

```properties
# application.properties

# Aumentar tempo de processamento
moondream.timeout=180

# Usar temperatura mais baixa (mais determinístico)
# No código: temperature: 0.1

# Prompt mais detalhado
# Já implementado no MoondreamService.java
```

---

## 🔐 Segurança e Privacidade

### **Vantagens:**
✅ Dados não saem da máquina local
✅ Imagens não são enviadas para APIs externas
✅ Conformidade com LGPD/GDPR
✅ Sem risco de vazamento de dados sensíveis

### **Ideal para:**
- 🏥 Dados médicos
- 🏦 Documentos financeiros
- 🔒 Informações confidenciais
- 🌐 Ambientes sem internet

---

## 📚 Recursos Adicionais

- 🌙 **Moondream:** https://moondream.ai/
- 🦙 **Ollama:** https://ollama.ai/
- 🐳 **Docker:** https://docs.docker.com/
- 📖 **Documentação completa:** QUICK_START.md

---

## ✅ Checklist de Sucesso

- [ ] Docker instalado e rodando
- [ ] Container moondream iniciado
- [ ] Modelo baixado (`ollama list`)
- [ ] API Ollama acessível (http://localhost:11434)
- [ ] Provedor configurado (`ai.provider=moondream`)
- [ ] Projeto compilado
- [ ] Aplicação rodando
- [ ] Teste bem-sucedido

---

**🎉 Pronto! Você agora tem análise de imagens offline com Moondream!**
