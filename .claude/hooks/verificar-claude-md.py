"""Antes de um commit, cobra a revisão do CLAUDE.md com base no que está no índice."""
import hashlib
import json
import os
import subprocess
import sys


def git(*args):
    saida = subprocess.run(
        ("git",) + args, capture_output=True, text=True, encoding="utf-8", errors="replace"
    )
    return saida.stdout if saida.returncode == 0 else ""


dados = json.loads(sys.stdin.buffer.read().decode("utf-8"))

arquivos = [linha for linha in git("diff", "--cached", "--name-only").splitlines() if linha]
if not arquivos or any(arquivo.endswith("CLAUDE.md") for arquivo in arquivos):
    raise SystemExit(0)

# O conteúdo do índice identifica a tentativa: reeditar um arquivo staged gera outra marca,
# e repetir o mesmo commit libera na segunda vez, então o gate nunca entra em laço.
marca = hashlib.sha256(git("diff", "--cached").encode("utf-8")).hexdigest()[:16]
os.makedirs(".claude/tmp", exist_ok=True)
registro = ".claude/tmp/claude-md-conferido-%s.txt" % dados.get("session_id", "sem-sessao")

vistas = set()
if os.path.exists(registro):
    with open(registro, encoding="utf-8") as arquivo:
        vistas = {linha.strip() for linha in arquivo}
if marca in vistas:
    raise SystemExit(0)
with open(registro, "a", encoding="utf-8") as arquivo:
    arquivo.write(marca + "\n")

motivo = (
    "Vão para o commit: " + "; ".join(sorted(arquivos)[:40]) + ". "
    "O CLAUDE.md não está entre eles. Verifique se ele ainda representa o estado atual do "
    "projeto: stack, comandos, convenções, estrutura e fluxo. Atualize apenas o que ficou "
    "desatualizado, descrevendo o estado atual. Não escreva motivo da mudança, como era antes, "
    "nem histórico. Se nada do que o CLAUDE.md documenta mudou, repita o commit: a segunda "
    "tentativa passa."
)
print(
    json.dumps(
        {
            "hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "deny",
                "permissionDecisionReason": motivo,
            }
        }
    )
)
