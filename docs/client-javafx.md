# UniBusiness JavaFX Client

Este documento explica como executar o cliente JavaFX do UniBusiness.

## Pré-requisitos

- JDK 17 ou superior instalado
- Maven 3.8+ instalado
- Docker instalado, se optar por rodar em contêiner
- No Linux, X11 configurado para exibir a interface gráfica via Docker

## Execução local

1. Abra um terminal na pasta do cliente:

```bash
cd client-javafx
```

2. Execute o cliente com Maven:

```bash
mvn clean javafx:run
```

Isso compila o projeto e inicia a aplicação JavaFX.

## Docker

O projeto `client-javafx` inclui um `Dockerfile` e um `Makefile` com tarefas para facilitar o uso de contêineres.

### Usando Makefile

No diretório `client-javafx`:

```bash
make build         # build da imagem Docker
make run           # roda o app localmente (sem Docker)
make docker-build  # build da imagem Docker
make docker-run    # executa o container Docker
make compose-up    # sobe o serviço via docker compose
make compose-down  # derruba o serviço Docker Compose
```

### Usando Docker diretamente

No diretório `client-javafx`:

```bash
docker build -t unibusiness-client .
```

Para executar o cliente em um contêiner Docker com exibição gráfica via X11:

```bash
docker run --rm -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix --network host unibusiness-client
```

### Usando Docker Compose

```bash
docker compose up --build
```

## Observações

- Se o servidor do UniBusiness estiver em outro contêiner ou máquina, ajuste a configuração de rede e a URL de conexão do cliente conforme necessário.
- Para um ambiente gráfico via Docker no Linux, confirme que o X11 está acessível e que o host permite conexões do contêiner.
