#!/usr/bin/env bash
# ============================================================================
# Remove os resquícios da versão ANTERIOR do projeto que podem ter sobrado
# ao copiar os arquivos novos por cima do projeto antigo.
#
# Causa típica do erro de compilação:
#   "com.itau.ingestor.dto.TransactionMessage cannot be converted to
#    com.itau.ingestor.message.TransactionMessage"
#
# Uso:  ./scripts/limpar-restos-antigos.sh
# ============================================================================
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1   # sobe para a raiz do projeto

echo "==> 1/3 Removendo pacotes da estrutura ANTIGA do ingestor..."
echo "    (dto, model, repository — os novos equivalentes ficam em"
echo "     message/ e persistence/)"
rm -rf ingestor/src/main/java/com/itau/ingestor/dto \
       ingestor/src/main/java/com/itau/ingestor/model \
       ingestor/src/main/java/com/itau/ingestor/repository

echo "==> 2/3 Removendo diretório de teste no lugar errado (src/main/test)..."
rm -rf api/src/main/test \
       ingestor/src/main/test

echo "==> 3/3 Limpando compilações antigas (classes obsoletas em target/)..."
rm -rf ingestor/target api/target db-migrations/target

echo ""
echo "==> Verificando se sobrou alguma referência ao pacote antigo 'ingestor.dto':"
if grep -rn "ingestor\.dto" ingestor/src api/src 2>/dev/null; then
    echo ""
    echo "!! Ainda existem referências ao pacote antigo acima."
    echo "   Isso significa que ALGUNS arquivos .java não foram substituídos"
    echo "   pela versão nova. Compare os arquivos listados com o projeto"
    echo "   fornecido e substitua-os."
else
    echo "    Nenhuma referência restante. OK!"
fi

echo ""
echo "Pronto! Agora rode:"
echo "    mvn clean test -Dgroups=unit"
