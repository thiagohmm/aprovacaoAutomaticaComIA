#!/bin/bash

echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                                                                    ║"
echo "║       🌙 Setup Moondream - Modelo de IA Offline (Docker)          ║"
echo "║                                                                    ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""

# Verificar se Docker está instalado
if ! command -v docker &> /dev/null; then
    echo "❌ Docker não está instalado!"
    echo "   Instale: https://docs.docker.com/get-docker/"
    exit 1
fi

echo "✅ Docker detectado"
echo ""

# Parar container existente se houver
echo "🛑 Parando container moondream anterior..."
docker stop moondream 2>/dev/null || true
docker rm moondream 2>/dev/null || true
echo ""

# Iniciar container Ollama
echo "🚀 Iniciando container Ollama..."
docker run -d \
  --name moondream \
  -p 11434:11434 \
  -v ollama-data:/root/.ollama \
  ollama/ollama:latest

if [ $? -ne 0 ]; then
    echo "❌ Erro ao iniciar container"
    exit 1
fi

echo "✅ Container iniciado"
echo ""

# Aguardar Ollama iniciar
echo "⏳ Aguardando Ollama iniciar (30 segundos)..."
sleep 30

# Verificar se está rodando
if ! docker ps | grep -q moondream; then
    echo "❌ Erro: Container moondream não está rodando"
    docker logs moondream
    exit 1
fi

echo "✅ Ollama rodando"
echo ""

# Baixar modelo Moondream
echo "📥 Baixando modelo Moondream (~2GB)..."
echo "   Isso pode levar alguns minutos..."
echo ""

docker exec moondream ollama pull moondream

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Modelo Moondream baixado com sucesso!"
else
    echo ""
    echo "❌ Erro ao baixar modelo Moondream"
    exit 1
fi

echo ""
echo "🧪 Testando Moondream..."
docker exec moondream ollama run moondream "Descreva brevemente: uma imagem de teste" --verbose

echo ""
echo "📋 Modelos instalados:"
docker exec moondream ollama list

echo ""
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                                                                    ║"
echo "║  ✅ Setup concluído com sucesso!                                  ║"
echo "║                                                                    ║"
echo "║  Container: moondream                                              ║"
echo "║  API: http://localhost:11434                                       ║"
echo "║                                                                    ║"
echo "║  Próximos passos:                                                  ║"
echo "║  1. Trocar provedor: ./switch-provider.sh moondream                ║"
echo "║  2. Compilar: mvn clean package                                    ║"
echo "║  3. Executar: java -jar target/auditoria-produtos-1.0.0.jar        ║"
echo "║                                                                    ║"
echo "║  Ou use: ./start-with-moondream.sh                                 ║"
echo "║                                                                    ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
