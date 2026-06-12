.PHONY: build run stop restart logs clean rebuild dev test-presence test-sotimeout test-sotime
 
# ── Configuração ──────────────────────────────────────────────────────────────
 
JAR         := target/uni-business.jar
SERVER_SRC  := src/main/java/com/unibusiness/network/ClientHandler.java
 
# ── Build ─────────────────────────────────────────────────────────────────────
 
# Compila o projeto Java (sem Docker)
build:
	mvn clean package -q
	@echo "✓ Build concluído: $(JAR)"
 
# Verifica se o ClientHandler tem setSoTimeout (sanity check)
check:
	@echo "=== Verificando ClientHandler compilado ==="
	@grep -n "setSoTimeout\|SocketTimeoutException" $(SERVER_SRC) \
		&& echo "✓ setSoTimeout encontrado no fonte" \
		|| echo "✗ setSoTimeout NÃO encontrado no fonte"
	@jar tf $(JAR) 2>/dev/null | grep ClientHandler \
		&& echo "✓ ClientHandler.class presente no jar" \
		|| echo "✗ jar não encontrado — rode: make build"
 
# ── Docker ────────────────────────────────────────────────────────────────────
 
# Sobe tudo do zero (força rebuild sem cache)
run: build
	docker compose down
	docker compose build --no-cache
	docker compose up -d
	@echo "✓ Servidor rodando. Logs: make logs"
 
# Para os containers
stop:
	docker compose down
	@echo "✓ Containers parados"
 
# Recompila Java + reconstrói imagem + reinicia (sem --no-cache — mais rápido)
restart: build
	docker compose down
	docker compose build
	docker compose up -d
	@echo "✓ Reiniciado"
 
# Rebuild completo: sem cache Maven nem Docker (use quando troca de arquivo não reflete)
rebuild: clean
	mvn package -q
	docker compose down
	docker compose build --no-cache
	docker compose up -d
	@echo "✓ Rebuild completo concluído"
 
# Mostra logs em tempo real
logs:
	docker compose logs -f app
 
# ── Dev local (sem Docker) ────────────────────────────────────────────────────
 
# Roda o servidor direto na máquina (precisa de .env exportado ou banco local)
dev: build
	@echo "=== Iniciando servidor local (sem Docker) ==="
	@set -a && . ./.env && set +a && java -jar $(JAR)
 
# ── Limpeza ───────────────────────────────────────────────────────────────────
 
clean:
	mvn clean -q
	@echo "✓ target/ limpo"
 
# Derruba containers E apaga volume do banco (reset total)
clean-all:
	docker compose down -v
	mvn clean -q
	@echo "✓ Containers, volumes e target/ limpos"
 