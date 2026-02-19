#!/bin/bash

echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                                                                    ║"
echo "║       🌙 Setup Moondream - Modelo de IA Offline                   ║"
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

# Verificar se docker-compose está instalado
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose não está instalado!"
    echo "   Instale: https://docs.docker.com/compose/install/"
    exit 1
fi

echo "✅ Docker Compose detectado"
echo ""

# Parar containers existentes
echo "🛑 Parando containers existentes..."
docker-compose down 2>/dev/null || true
echo ""

# Iniciar container Ollama
echo "🚀 Iniciando container Ollama..."
docker-compose up -d moondream

# Aguardar Ollama iniciar
echo "⏳ Aguardando Ollama iniciar (30 segundos)..."
sleep 30

# Verificar se Ollama está rodando
if ! docker ps | grep -q moondream; then
    echo "❌ Erro: Container Moondream não está rodando"
    exit 1
fi

echo "✅ Ollama rodando"
echo ""

# Baixar modelo Moondream
echo "📥 Baixando modelo Moondream (~2GB)..."
echo "   Isso pode levar alguns minutos..."
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
docker exec moondream ollama run moondream "Descreva brevemente: uma imagem de teste"

echo ""
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                                                                    ║"
echo "║  ✅ Setup concluído com sucesso!                                  ║"
echo "║                                                                    ║"
echo "║  Próximos passos:                                                  ║"
echo "║  1. Configure: ai.provider=moondream                               ║"
echo "║  2. Execute: ./start-with-moondream.sh                             ║"
echo "║  3. Teste a API: curl http://localhost:8080/api/v1/auditoria/health║"
echo "║                                                                    ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
