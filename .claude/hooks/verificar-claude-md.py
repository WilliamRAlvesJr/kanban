"""Antes de um commit, cobra a revisão do CLAUDE.md com base no que está no índice."""
import hashlib
import json
import os
import re
import subprocess
import sys


def git(*args):
    saida = subprocess.run(
        ("git",) + args, capture_output=True, text=True, encoding="utf-8", errors="replace"
    )
    return saida.stdout if saida.returncode == 0 else ""


def negar(motivo):
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
    raise SystemExit(0)


def adiciona_na_mesma_chamada(comando):
    commit = re.search(r"\bgit\s+commit\b", comando)
    if not commit:
        return False
    if re.search(r"\bgit\s+add\b", comando[: commit.start()]):
        return True
    for opcao in re.match(r"(?:\s+-\S*)*", comando[commit.end():]).group().split():
        if opcao == "--all" or re.fullmatch(r"-[A-Za-z]*a[A-Za-z]*", opcao):
            return True
    return False


dados = json.loads(sys.stdin.buffer.read().decode("utf-8"))

# O hook roda antes do comando: git add encadeado ou commit -a ainda não mudaram o índice.
if adiciona_na_mesma_chamada(dados.get("tool_input", {}).get("command", "")):
    negar(
        "O git add e o git commit estão na mesma chamada, então o índice ainda não reflete o "
        "commit. Rode o git add numa chamada separada e depois o git commit, sem -a."
    )

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

negar(
    "Vão para o commit: " + "; ".join(sorted(arquivos)[:40]) + ". "
    "O CLAUDE.md não está entre eles. Verifique se ele ainda representa o estado atual do "
    "projeto: stack, comandos, convenções, estrutura e fluxo. Atualize apenas o que ficou "
    "desatualizado, descrevendo o estado atual. Não escreva motivo da mudança, como era antes, "
    "nem histórico. Se nada do que o CLAUDE.md documenta mudou, repita o commit: a segunda "
    "tentativa passa."
)
