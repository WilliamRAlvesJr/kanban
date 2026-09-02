"""Registra em .claude/tmp os arquivos que o agente cria, altera ou exclui no turno."""
import json
import os
import re
import sys

ESCRITA_BASH = re.compile(
    r"(^|[;&|]\s*)(rm|mv|cp|touch|tee|dd)\s"
    r"|\bsed\s+-i\b"
    r"|\bRemove-Item\b"
    r"|(?<![0-9&])>{1,2}\s*(?!/dev/null|\$null)",
)


def alvo(dados):
    entrada = dados.get("tool_input") or {}
    if dados.get("tool_name") in ("Edit", "Write", "NotebookEdit"):
        return entrada.get("file_path")
    comando = (entrada.get("command") or "").strip()
    if comando and ESCRITA_BASH.search(comando):
        return "comando: " + " ".join(comando.split())[:160]
    return None


dados = json.loads(sys.stdin.buffer.read().decode("utf-8"))
caminho = alvo(dados)
if not caminho or caminho.endswith("CLAUDE.md"):
    raise SystemExit(0)

os.makedirs(".claude/tmp", exist_ok=True)
registro = ".claude/tmp/claude-md-pendente-%s.txt" % dados.get("session_id", "sem-sessao")
with open(registro, "a", encoding="utf-8") as arquivo:
    arquivo.write(caminho + "\n")
