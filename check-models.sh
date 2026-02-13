#!/bin/bash

# Script para listar modelos disponíveis na API do Gemini

API_KEY="AIzaSyBK0ibJdcCqMj4i3XIf5Cp_nqb-Z-iXwG4"

echo "=========================================="
echo "  Verificando Modelos Gemini Disponíveis"
echo "=========================================="
echo ""

echo "🔍 Consultando API do Gemini..."
echo ""

# Listar modelos disponíveis
curl -s "https://generativelanguage.googleapis.com/v1beta/models?key=${API_KEY}" \
  | jq -r '.models[] | select(.supportedGenerationMethods[] | contains("generateContent")) | "\(.name) - \(.displayName)"' \
  2>/dev/null || curl -s "https://generativelanguage.googleapis.com/v1beta/models?key=${API_KEY}"

echo ""
echo "=========================================="
echo "✅ Consulta concluída"
echo "=========================================="
