# Testing Guide - Fixed API Script

## Quick Start

### 1. Make the script executable

```bash
chmod +x test-api.sh
```

### 2. Ensure the API is running

```bash
# In another terminal, start the Spring Boot application
mvn spring-boot:run
```

### 3. Run the test script

```bash
./test-api.sh
```

## What to Expect

### ✅ Successful Output

You should see:

```
==========================================
  API de Auditoria - Script de Teste
==========================================

🔍 Verificando status da API...
✅ API está funcionando!

==========================================
  Teste 1: Envio com 2 imagens
==========================================

🔄 Enviando 2 imagens (xequemate.jpg e xequemateBack.jpg)...
📦 Tamanho das imagens: 280K + 281K

✅ Requisição bem-sucedida (HTTP 200)
📄 Resposta salva em: ./api-responses/teste1_2imagens_response.json
📊 Tamanho da resposta: 4.2K

📋 Conteúdo da resposta:
----------------------------------------
{
  "idSolicitacao": 30470331,
  "status": "APROVADO",
  "motivo": "...",
  "timestamp": "..."
}
----------------------------------------
```

### ❌ No More "Failed writing body" Error

The old error:

```
curl: Failed writing body
```

**Should NOT appear anymore!**

## Viewing Saved Responses

All responses are saved in `./api-responses/` directory:

```bash
# List all saved responses
ls -lh ./api-responses/

# View a specific response (formatted)
jq '.' ./api-responses/teste1_2imagens_response.json

# View without jq
cat ./api-responses/teste1_2imagens_response.json
```

## Troubleshooting

### Issue: API not responding

```
❌ API não está respondendo (HTTP 000)
```

**Solution:** Make sure the Spring Boot application is running on port 8080

### Issue: Images not found

```
⚠️  Imagens xequemate.jpg ou xequemateBack.jpg não encontradas
```

**Solution:** Place the image files in the same directory as the script

### Issue: HTTP 400 Bad Request

**Possible causes:**

- Invalid JSON in the request
- Missing required fields
- Image files corrupted

**Check the saved response:**

```bash
cat ./api-responses/teste1_2imagens_response.json
```

### Issue: HTTP 500 Internal Server Error

**Possible causes:**

- Gemini API key invalid or missing
- Network issues connecting to Gemini API
- Server-side error processing images

**Check application logs:**

```bash
# In the terminal running the Spring Boot app
# Look for ERROR or WARN messages
```

## Testing Different Scenarios

### Test with specific images

```bash
# Place your images in the current directory
cp /path/to/your/image1.jpg ./xequemate.jpg
cp /path/to/your/image2.jpg ./xequemateBack.jpg

# Run the script
./test-api.sh
```

### Test with multiple images

The script automatically detects all images in the current directory for Test 2:

```bash
# Copy multiple images to current directory
cp /path/to/images/*.jpg ./

# Run the script - Test 2 will use all images
./test-api.sh
```

### Manual curl test

```bash
# Test manually with curl
curl -X POST "http://localhost:8080/api/v1/auditoria/produtos" \
  -F "imagens=@xequemate.jpg" \
  -F "imagens=@xequemateBack.jpg" \
  -F 'dados={"IdSolicitacao": 12345, "DescricaoProduto": "Test Product", "codigosDeBarras": [{"codigoBarras": "123456789"}]}' \
  -H "Accept: application/json" \
  -o response.json \
  -w "\nHTTP Status: %{http_code}\n"

# View the response
cat response.json | jq '.'
```

## Performance Monitoring

### Check response sizes

```bash
# See how large the responses are
du -h ./api-responses/*.json
```

### Monitor API performance

```bash
# Time the request
time ./test-api.sh
```

### Check compression

```bash
# Verify compression is working (response should be smaller)
curl -X POST "http://localhost:8080/api/v1/auditoria/produtos" \
  -F "imagens=@xequemate.jpg" \
  -F "imagens=@xequemateBack.jpg" \
  -F 'dados={"IdSolicitacao": 12345, "DescricaoProduto": "Test", "codigosDeBarras": [{"codigoBarras": "123"}]}' \
  -H "Accept: application/json" \
  -H "Accept-Encoding: gzip" \
  -v 2>&1 | grep -i "content-encoding"
```

## Cleanup

### Remove saved responses

```bash
rm -rf ./api-responses/
```

### Remove test images

```bash
rm -f xequemate.jpg xequemateBack.jpg
```

## Success Criteria

✅ Script runs without "Failed writing body" error
✅ Responses are saved to files successfully
✅ HTTP 200 status code received
✅ JSON response is properly formatted
✅ API returns APROVADO or REPROVADO status
✅ Response includes detailed motivo (reason)

## Next Steps After Testing

1. ✅ Verify the fix works with your actual images
2. ✅ Check that the API responses are correct
3. ✅ Monitor performance with large images
4. ✅ Consider adding more test cases if needed
5. ✅ Deploy to production if all tests pass
