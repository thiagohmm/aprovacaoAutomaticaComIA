#!/usr/bin/env python3
"""
Script de exemplo para testar a API de Auditoria de Produtos
Demonstra como enviar múltiplas imagens para a API
"""

import requests
import json
import sys
from pathlib import Path

# URL da API
API_URL = "http://localhost:8080/api/v1/auditoria/produtos"

def auditar_produto_com_2_imagens():
    """Exemplo com 2 imagens (frente e verso)"""
    
    # Dados do produto
    dados = {
        "IdSolicitacao": 30470331,
        "DescricaoProduto": "Xeque mate energetico",
        "codigosDeBarras": [
            {"codigoBarras": "121313"}
        ]
    }
    
    # Preparar arquivos
    files = [
        ('imagens', ('XEQUEMATE.jpg', open('XEQUEMATE.jpg', 'rb'), 'image/jpeg')),
        ('imagens', ('XEQUEMATECB.jpg', open('XEQUEMATECB.jpg', 'rb'), 'image/jpeg'))
    ]
    
    # Preparar dados
    form_data = {
        'dados': json.dumps(dados)
    }
    
    print("🔄 Enviando requisição com 2 imagens...")
    
    try:
        response = requests.post(API_URL, files=files, data=form_data)
        
        if response.status_code == 200:
            print("✅ Auditoria realizada com sucesso!\n")
            print(json.dumps(response.json(), indent=2, ensure_ascii=False))
        else:
            print(f"❌ Erro: {response.status_code}")
            print(response.text)
            
    except Exception as e:
        print(f"❌ Erro ao fazer requisição: {e}")
    finally:
        # Fechar arquivos
        for _, file_tuple in files:
            file_tuple[1].close()


def auditar_produto_com_multiplas_imagens():
    """Exemplo com múltiplas imagens"""
    
    # Dados do produto
    dados = {
        "IdSolicitacao": 30470332,
        "DescricaoProduto": "Produto teste",
        "codigosDeBarras": [
            {"codigoBarras": "789456123"}
        ]
    }
    
    # Lista de imagens para enviar
    imagens_paths = [
        "frente.jpg",
        "verso.jpg",
        "lateral1.jpg",
        "lateral2.jpg",
        "codigo-barras.jpg"
    ]
    
    # Preparar arquivos (apenas os que existem)
    files = []
    for img_path in imagens_paths:
        if Path(img_path).exists():
            files.append(
                ('imagens', (img_path, open(img_path, 'rb'), 'image/jpeg'))
            )
    
    if not files:
        print("❌ Nenhuma imagem encontrada! Coloque as imagens no mesmo diretório do script.")
        return
    
    # Preparar dados
    form_data = {
        'dados': json.dumps(dados)
    }
    
    print(f"🔄 Enviando requisição com {len(files)} imagens...")
    
    try:
        response = requests.post(API_URL, files=files, data=form_data)
        
        if response.status_code == 200:
            print("✅ Auditoria realizada com sucesso!\n")
            print(json.dumps(response.json(), indent=2, ensure_ascii=False))
        else:
            print(f"❌ Erro: {response.status_code}")
            print(response.text)
            
    except Exception as e:
        print(f"❌ Erro ao fazer requisição: {e}")
    finally:
        # Fechar arquivos
        for _, file_tuple in files:
            file_tuple[1].close()


def verificar_health():
    """Verifica se a API está funcionando"""
    try:
        response = requests.get("http://localhost:8080/api/v1/auditoria/health")
        if response.status_code == 200:
            print("✅ API está funcionando!")
            print(f"   {response.text}")
            return True
        else:
            print(f"❌ API retornou status: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ Não foi possível conectar à API: {e}")
        print("   Certifique-se de que a API está rodando em http://localhost:8080")
        return False


if __name__ == "__main__":
    print("=" * 60)
    print("  API de Auditoria de Produtos - Script de Teste")
    print("=" * 60)
    print()
    
    # Verificar se a API está rodando
    if not verificar_health():
        sys.exit(1)
    
    print("\n" + "=" * 60)
    print("  Teste 1: Envio com 2 imagens")
    print("=" * 60 + "\n")
    auditar_produto_com_2_imagens()
    
    print("\n" + "=" * 60)
    print("  Teste 2: Envio com múltiplas imagens")
    print("=" * 60 + "\n")
    auditar_produto_com_multiplas_imagens()
    
    print("\n✨ Testes concluídos!")
