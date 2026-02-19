#!/bin/bash

# Script de teste da API de Auditoria de Produtos
# Demonstra como enviar múltiplas imagens usando curl
# Versão corrigida: resolve o erro "Failed writing body"

API_URL="http://localhost:8080/api/v1/auditoria"
RESPONSE_DIR="./api-responses"

# Cores para output (opcional)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Criar diretório para respostas se não existir
mkdir -p "$RESPONSE_DIR"

echo "=========================================="
echo "  API de Auditoria - Script de Teste"
echo "=========================================="
echo ""

# Verificar health da API
echo -e "${BLUE}🔍 Verificando status da API...${NC}"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "${API_URL}/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ API está funcionando!${NC}"
    echo ""
else
    echo -e "${RED}❌ API não está respondendo (HTTP $HTTP_CODE)${NC}"
    echo "   Certifique-se de que a aplicação está rodando em http://localhost:8080"
    exit 1
fi

# Função para formatar JSON (tenta usar jq, senão usa python, senão mostra raw)
format_json() {
    local json_file="$1"
    
    if command -v jq &> /dev/null; then
        jq '.' "$json_file" 2>/dev/null
    elif command -v python3 &> /dev/null; then
        python3 -m json.tool "$json_file" 2>/dev/null
    else
        cat "$json_file"
    fi
}

# Função para fazer requisição e salvar resposta
make_request() {
    local test_name="$1"
    local response_file="${RESPONSE_DIR}/${test_name}_response.json"
    local temp_file="${response_file}.tmp"
    
    shift # Remove o primeiro argumento (test_name)
    
    # Fazer a requisição e salvar em arquivo temporário
    # Separar o corpo da resposta do código HTTP
    HTTP_CODE=$(curl -s -w "%{http_code}" -o "$temp_file" "$@")
    
    # Verificar se a requisição foi bem-sucedida
    if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
        mv "$temp_file" "$response_file"
        echo -e "${GREEN}✅ Requisição bem-sucedida (HTTP $HTTP_CODE)${NC}"
        echo -e "${BLUE}📄 Resposta salva em: $response_file${NC}"
        
        # Mostrar tamanho da resposta
        local file_size=$(du -h "$response_file" | cut -f1)
        echo -e "${BLUE}📊 Tamanho da resposta: $file_size${NC}"
        echo ""
        
        # Tentar formatar e mostrar a resposta
        echo -e "${BLUE}📋 Conteúdo da resposta:${NC}"
        echo "----------------------------------------"
        format_json "$response_file"
        echo "----------------------------------------"
        
        return 0
    else
        echo -e "${RED}❌ Erro na requisição (HTTP $HTTP_CODE)${NC}"
        if [ -f "$temp_file" ]; then
            echo -e "${RED}Detalhes do erro:${NC}"
            cat "$temp_file"
            rm "$temp_file"
        fi
        return 1
    fi
}

# Teste 1: Envio com 2 imagens
echo "=========================================="
echo "  Teste 1: Envio com 2 imagens"
echo "=========================================="
echo ""

DADOS_JSON='{
  "IdSolicitacao": 30470331,
  "DescricaoProduto": "Xequemate 10L",
  "codigosDeBarras": [
    {"codigoBarras": "7898190225388"}
  ]
}'

if [ -f "xequemate.jpg" ] && [ -f "xequemateBack.jpg" ]; then
    echo -e "${BLUE}🔄 Enviando 2 imagens (xequemate.jpg e xequemateBack.jpg)...${NC}"
    
    # Mostrar tamanho das imagens
    size1=$(du -h "xequemate.jpg" | cut -f1)
    size2=$(du -h "xequemateBack.jpg" | cut -f1)
    echo -e "${BLUE}📦 Tamanho das imagens: $size1 + $size2${NC}"
    echo ""
    
    make_request "teste1_2imagens" \
      -X POST "${API_URL}/produtos" \
      -F "imagens=@xequemate.jpg" \
      -F "imagens=@xequemateBack.jpg" \
      -F "dados=${DADOS_JSON}" \
      -H "Accept: application/json"
    
    echo ""
else
    echo -e "${YELLOW}⚠️  Imagens xequemate.jpg ou xequemateBack.jpg não encontradas${NC}"
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
IMAGE_LIST=""

for IMG in *.*; do
    if [[ -f "$IMG" && $(file --mime-type -b "$IMG" 2>/dev/null) == image/* ]]; then
        CURL_PARAMS="$CURL_PARAMS -F imagens=@$IMG"
        IMAGE_LIST="$IMAGE_LIST\n  - $IMG ($(du -h "$IMG" | cut -f1))"
        ((IMAGE_COUNT++))
    fi
done

if [ $IMAGE_COUNT -gt 0 ]; then
    echo -e "${BLUE}🔄 Enviando $IMAGE_COUNT imagens:${NC}"
    echo -e "$IMAGE_LIST"
    echo ""
    
    make_request "teste2_multiplas" \
      -X POST "${API_URL}/produtos" \
      $CURL_PARAMS \
      -F "dados=${DADOS_JSON2}" \
      -H "Accept: application/json"
    
    echo ""
else
    echo -e "${YELLOW}⚠️  Nenhuma imagem encontrada no diretório atual${NC}"
    echo ""
fi

echo "=========================================="
echo -e "${GREEN}✨ Testes concluídos!${NC}"
echo "=========================================="
echo ""
echo -e "${BLUE}📁 Todas as respostas foram salvas em: $RESPONSE_DIR/${NC}"
echo ""
