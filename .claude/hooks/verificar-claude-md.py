"""Ao fim do turno, cobra a verificação do CLAUDE.md se algum arquivo mudou."""
import json
import os
import sys

dados = json.loads(sys.stdin.buffer.read().decode("utf-8"))
registro = ".claude/tmp/claude-md-pendente-%s.txt" % dados.get("session_id", "sem-sessao")
if not os.path.exists(registro):
    raise SystemExit(0)

with open(registro, encoding="utf-8") as arquivo:
    itens = sorted({linha.strip() for linha in arquivo if linha.strip()})
os.remove(registro)  # apagar antes de bloquear evita laço no Stop seguinte
if not itens:
    raise SystemExit(0)

motivo = (
    "Mudou neste turno: " + "; ".join(itens[:40]) + ". "
    "Verifique se o CLAUDE.md ainda representa o estado atual do projeto: stack, comandos, "
    "convenções, estrutura e fluxo. Atualize apenas o que ficou desatualizado, descrevendo o "
    "estado atual. Não escreva motivo da mudança, como era antes, nem histórico. Se nada do que "
    "o CLAUDE.md documenta mudou, não edite o arquivo e encerre."
)
print(json.dumps({"decision": "block", "reason": motivo}))
