#!/usr/bin/env bash
# Equivalente a validar-http.ps1. Requer Bash 3.2+, curl e Python 3.
set -euo pipefail
base_url='http://localhost:9091'
falha_externa=false
while (($#)); do
    case "$1" in
        --base-url) [[ $# -ge 2 && -n "$2" ]] || { echo 'Informe --base-url URL' >&2; exit 2; }; base_url=${2%/}; shift 2 ;;
        --falha-externa) falha_externa=true; shift ;;
        -h|--help) echo 'Uso: bash scripts/validar-http.sh [--base-url URL] [--falha-externa]'; exit 0 ;;
        *) echo "Argumento desconhecido: $1" >&2; exit 2 ;;
    esac
done
for ferramenta in curl python3; do
    command -v "$ferramenta" >/dev/null || { echo "Dependência ausente: $ferramenta" >&2; exit 1; }
done
trabalho=$(mktemp -d)
id=''
requisicao() {
    local metodo=$1 rota=$2 esperado=$3 json=${4-} status
    local argumentos=(--silent --show-error --connect-timeout 5 --max-time 20)
    if [[ $# -ge 4 ]]; then argumentos+=(-H 'Content-Type: application/json' --data-binary "$json"); fi
    status=$(curl \
        -X "$metodo" -D "$trabalho/headers" -o "$trabalho/body" -w '%{http_code}' \
        "${argumentos[@]}" "$base_url$rota") || return 1
    if [[ "$status" != "$esperado" ]]; then
        echo "$metodo $rota esperado=$esperado recebido=$status: $(cat "$trabalho/body")" >&2
        return 1
    fi
    if ((esperado >= 400)); then
        python3 -c 'import json,sys; e=json.load(open(sys.argv[1])); assert isinstance(e,dict) and all(k in e for k in ("codigo","mensagem","timestamp","path","detalhes")), "Erro sem campos obrigatórios"' "$trabalho/body" || return 1
    fi
    echo "OK $metodo $rota -> $esperado"
}
limpar() {
    local resultado=$?
    trap - EXIT
    if [[ -n "$id" ]]; then
        requisicao DELETE "/agendamentos/$id" 204 || echo 'Aviso: não foi possível remover a reserva temporária.' >&2
    fi
    rm -rf -- "$trabalho"
    exit "$resultado"
}
trap limpar EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
contador() {
    requisicao GET '/q/metrics' 200 >&2
    python3 -c 'import re,sys; from decimal import Decimal; m=re.search(r"^agendamentos_criados_total\s+([\d.eE+-]+)",open(sys.argv[1]).read(),re.M); assert m, "Contador ausente"; print(Decimal(m[1]))' "$trabalho/body"
}
comparar_contador() {
    local atual
    atual=$(contador)
    python3 -c 'from decimal import Decimal; import sys; assert Decimal(sys.argv[1]) == Decimal(sys.argv[2])+int(sys.argv[3]), "Contador inesperado"' "$atual" "$antes" "$1"
}
ano=$(python3 -c 'from datetime import date; print(date.today().year+1)')
sala=$(python3 -c 'import secrets; print(100000+secrets.randbelow(999900000))')
dados() {
    printf '{"id_sala":%s,"id_usuario":10,"uf":"SP","data":"%s","periodo":"%s"}' "$sala" "$1" "$2"
}
json=$(dados "$ano-02-10" MANHA)
if "$falha_externa"; then
    antes=$(contador)
    requisicao POST '/agendamentos' 503 "$json"
    comparar_contador 0
    requisicao GET '/agendamentos' 200
    python3 -c 'import json,sys; assert not any(r["id_sala"] == int(sys.argv[2]) for r in json.load(open(sys.argv[1]))), "Falha externa gravou reserva"' "$trabalho/body" "$sala"
    echo 'Cenário de indisponibilidade verificado.'
    exit 0
fi
requisicao GET '/info' 200
requisicao GET '/q/health/live' 200
requisicao GET '/q/health/ready' 200
antes=$(contador)
requisicao POST '/agendamentos' 201 "$json"
id=$(python3 -c 'import json,sys; v=json.load(open(sys.argv[1]))["id"]; assert type(v) is int and v>0; print(v)' "$trabalho/body")
python3 -c 'import sys; from email.parser import Parser; h=Parser().parsestr(open(sys.argv[1]).read().split("\n",1)[1]); assert h.get("Location", "").endswith("/agendamentos/"+sys.argv[2]), "Location incorreto"' "$trabalho/headers" "$id"
comparar_contador 1
requisicao GET '/agendamentos' 200
requisicao GET "/agendamentos/$id" 200
requisicao POST '/agendamentos' 409 "$json"
requisicao PUT "/agendamentos/$id" 200 "$json"
json=$(dados "$ano-02-10" TARDE)
requisicao PUT "/agendamentos/$id" 200 "$json"
requisicao GET "/agendamentos/$id" 200
python3 -c 'import json,sys; assert json.load(open(sys.argv[1]))["periodo"] == "TARDE", "PUT não persistiu"' "$trabalho/body"
requisicao POST '/agendamentos' 400 '{}'
requisicao POST '/agendamentos' 400 '{'
requisicao POST '/agendamentos' 409 "$(dados "$ano-01-01" TARDE)"
comparar_contador 1
requisicao DELETE "/agendamentos/$id" 204
requisicao GET "/agendamentos/$id" 404
requisicao DELETE "/agendamentos/$id" 404
id=''
echo 'Fluxo HTTP completo verificado.'
