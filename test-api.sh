#!/bin/bash

# Script de teste da API de Auditoria de Produtos
# Demonstra como enviar múltiplas imagens usando curl

API_URL="http://localhost:8080/api/v1/auditoria"

echo "=========================================="
echo "  API de Auditoria - Script de Teste"
echo "=========================================="
echo ""

# Verificar health da API
echo "🔍 Verificando status da API..."
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "${API_URL}/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ API está funcionando!"
    echo ""
else
    echo "❌ API não está respondendo (HTTP $HTTP_CODE)"
    echo "   Certifique-se de que a aplicação está rodando em http://localhost:8080"
    exit 1
fi

# Teste 1: Envio com 2 imagens
echo "=========================================="
echo "  Teste 1: Envio com 2 imagens"
echo "=========================================="
echo ""

DADOS_JSON='{
  "IdSolicitacao": 30470331,
  "DescricaoProduto": "Xequemate 10L" ,
  "codigosDeBarras": [
    {"codigoBarras": "7898190225388"}
  ]
}'

if [ -f "XEQUEMATE.jpg" ] && [ -f "XEQUEMATECB.jpg" ]; then
    echo "🔄 Enviando 2 imagens..."
    curl -X POST "${API_URL}/produtos" \
      -F "imagens=@XEQUEMATE.jpg" \
      -F "imagens=@XEQUEMATECB.jpg" \
      -F "dados=${DADOS_JSON}" \
      -H "Accept: application/json" \
      -w "\n\nHTTP Status: %{http_code}\n" \
      | jq '.' 2>/dev/null || cat
    echo ""
else
    echo "⚠️  Imagens XEQUEMATE.jpg ou XEQUEMATECB.jpg não encontradas"
    echo ""
fi

# Teste 2: Envio com múltiplas imagens
echo "=========================================="
echo "  Teste 2: Envio com múltiplas imagens"
echo "=========================================="
echo ""

DADOS_JSON2='{
  "IdSolicitacao": 30470332,
  "DescricaoProduto": "Produto com multiplas fotos",
  "codigosDeBarras": [
    {"codigoBarras": "789456123"}
  ]
}'

# Verificar todas as imagens na pasta atual (qualquer extensão de imagem)
CURL_PARAMS=""
IMAGE_COUNT=0

for IMG in *.*; do
    if [[ -f "$IMG" && $(file --mime-type -b "$IMG") == image/* ]]; then
        CURL_PARAMS="$CURL_PARAMS -F imagens=@$IMG"
        ((IMAGE_COUNT++))
    fi
done

if [ $IMAGE_COUNT -gt 0 ]; then
    echo "🔄 Enviando $IMAGE_COUNT imagens..."
    curl -X POST "${API_URL}/produtos" \
      $CURL_PARAMS \
      -F "dados=${DADOS_JSON2}" \
      -H "Accept: application/json" \
      -w "\n\nHTTP Status: %{http_code}\n" \
      | jq '.' 2>/dev/null || cat
    echo ""
else
    echo "⚠️  Nenhuma imagem encontrada no diretório atual"
    echo ""
fi

echo "=========================================="
echo "✨ Testes concluídos!"
echo "=========================================="
