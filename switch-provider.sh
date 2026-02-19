#!/bin/bash

# Script para alternar entre provedores de IA
# Uso: ./switch-provider.sh [gemini|deepseek|moondream]

PROPERTIES_FILE="src/main/resources/application.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
    echo "❌ Erro: application.properties não encontrado!"
    exit 1
fi

# Verificar argumento
if [ $# -eq 0 ]; then
    # Mostrar provedor atual
    CURRENT=$(grep "^ai.provider=" "$PROPERTIES_FILE" | cut -d'=' -f2)
    echo "🔍 Provedor atual: $CURRENT"
    echo ""
    echo "📝 Uso: $0 [gemini|deepseek|moondream]"
    echo ""
    echo "Exemplos:"
    echo "  $0 gemini      # Trocar para Gemini (online, mais preciso)"
    echo "  $0 deepseek    # Trocar para DeepSeek (online, apenas texto)"
    echo "  $0 moondream   # Trocar para Moondream (offline, com imagens)"
    exit 0
fi

PROVIDER=$1

# Validar provedor
if [ "$PROVIDER" != "gemini" ] && [ "$PROVIDER" != "deepseek" ] && [ "$PROVIDER" != "moondream" ]; then
    echo "❌ Erro: Provedor inválido!"
    echo "   Opções: gemini, deepseek, moondream"
    exit 1
fi

# Fazer backup
cp "$PROPERTIES_FILE" "$PROPERTIES_FILE.bak"

# Alterar provedor
sed -i "s/^ai.provider=.*/ai.provider=$PROVIDER/" "$PROPERTIES_FILE"

CURRENT=$(grep "^ai.provider=" "$PROPERTIES_FILE" | cut -d'=' -f2)

echo "✅ Provedor alterado para: $CURRENT"
echo ""
echo "📋 Próximos passos:"

if [ "$PROVIDER" = "gemini" ]; then
    echo "   ✓ Gemini suporta análise de imagens"
    KEY=$(grep "^gemini.api.key=" "$PROPERTIES_FILE" | cut -d'=' -f2)
    if [ "$KEY" = "SUA_CHAVE_GEMINI_AQUI" ] || [ -z "$KEY" ]; then
        echo "   ⚠️  Configure: gemini.api.key no application.properties"
    else
        echo "   ✓ Chave Gemini configurada"
    fi
elif [ "$PROVIDER" = "deepseek" ]; then
    echo "   ⚠️  DeepSeek NÃO suporta análise de imagens!"
    echo "   ⚠️  Use Gemini ou Moondream para análise visual"
    KEY=$(grep "^deepseek.api.key=" "$PROPERTIES_FILE" | cut -d'=' -f2)
    if [ "$KEY" = "SUA_CHAVE_DEEPSEEK_AQUI" ] || [ -z "$KEY" ]; then
        echo "   ⚠️  Configure: deepseek.api.key no application.properties"
    else
        echo "   ✓ Chave DeepSeek configurada"
    fi
elif [ "$PROVIDER" = "moondream" ]; then
    echo "   🌙 Moondream roda offline (via Docker)"
    echo "   ✓ Suporta análise de imagens"
    if ! docker ps | grep -q moondream; then
        echo "   ⚠️  Container Moondream não está rodando"
        echo "   💡 Execute: ./setup-moondream.sh"
    else
        echo "   ✓ Container Moondream rodando"
    fi
fi

echo "   2. Recompile o projeto: mvn clean package"
echo "   3. Reinicie a aplicação"
echo ""
echo "💡 Backup salvo em: $PROPERTIES_FILE.bak"
