#!/bin/bash

echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                                                                    ║"
echo "║    🚀 Iniciando Sistema de Auditoria com Moondream (Offline)      ║"
echo "║                                                                    ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""

# Configurar provedor para Moondream
echo "⚙️  Configurando provedor para Moondream..."
sed -i.bak 's/^ai.provider=.*/ai.provider=moondream/' src/main/resources/application.properties

# Adicionar configurações do Moondream se não existirem
if ! grep -q "moondream.api.url" src/main/resources/application.properties; then
    echo "" >> src/main/resources/application.properties
    echo "# Configuração do Moondream (Offline)" >> src/main/resources/application.properties
    echo "moondream.api.url=http://localhost:11434" >> src/main/resources/application.properties
    echo "moondream.model=moondream" >> src/main/resources/application.properties
fi

echo "✅ Configuração atualizada"
echo ""

# Compilar projeto
echo "🔨 Compilando projeto..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Erro na compilação"
    exit 1
fi

echo "✅ Compilação concluída"
echo ""

# Verificar se Moondream está rodando
if ! docker ps | grep -q moondream; then
    echo "⚠️  Container Moondream não está rodando"
    echo "   Executando setup..."
    ./setup-moondream.sh
fi

echo "🚀 Iniciando aplicação..."
echo ""
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║  API: http://localhost:8080/api                                    ║"
echo "║  Health: http://localhost:8080/api/v1/auditoria/health             ║"
echo "║  Ollama: http://localhost:11434                                    ║"
echo "║                                                                    ║"
echo "║  Pressione Ctrl+C para parar                                       ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""

# Iniciar aplicação
java -jar target/auditoria-produtos-*.jar
