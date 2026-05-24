# Como rodar o servidor

## Quick start (recomendado)

1. Copie `.env.example` para `.env`:
```bash
cp .env.example .env
```

2. Suba app + banco com docker-compose:
```bash
make compose-up
```

Pronto! O servidor está rodando.

## Alternativas

### Rodar localmente (sem Docker)

```bash
# Subir só o banco
docker run --name unibusiness-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=unibusiness -p 5432:5432 -d postgres:15

# Em outro terminal, rodar a app
make run-local
```

### Usar Docker para a app

```bash
make docker-run
```

## Comandos úteis

```bash
# Compilar JAR
make mvn-build

# Rodar locally a partir do JAR
make run-local

# Subir só DB
make db-run

# Build imagem Docker
make docker-build

# Rodar imagem Docker
make docker-run

# Compose up (app + db)
make compose-up

# Compose down
make compose-down

# Limpar
make clean
```

## Notas

- As variáveis de ambiente são lidas de `.env` (que deve estar na raiz).
- O JPA cria/atualiza as tabelas automaticamente (`hibernate.hbm2ddl.auto=update`).
- O database `unibusiness` é criado automaticamente pelo PostgreSQL ao iniciar.
