# UniBusiness — Camada de Rede TCP

Documentação da camada de conexão TCP adicionada ao projeto **UniBusiness**.  
Implementa comunicação cliente-servidor via JSON sobre TCP, com suporte a CRUD de todas as entidades e push de mensagens em tempo real.

---

## Sumário

1. [Visão Geral](#visão-geral)
2. [Arquitetura](#arquitetura)
3. [Dependências](#dependências)
4. [Configuração e Inicialização](#configuração-e-inicialização)
5. [Protocolo de Comunicação](#protocolo-de-comunicação)
6. [Autenticação](#autenticação)
7. [Referência de Actions](#referência-de-actions)
8. [Push em Tempo Real](#push-em-tempo-real)
9. [Descrição dos Arquivos](#descrição-dos-arquivos)
10. [Exemplo de Cliente](#exemplo-de-cliente)
11. [Segurança — Próximos Passos](#segurança--próximos-passos)

---

## Visão Geral

A camada de rede implementa um servidor TCP multithreaded. Cada cliente mantém uma **conexão persistente** com o servidor. A comunicação é feita por troca de mensagens JSON, uma por linha (`\n` como delimitador de frame).

**Por que TCP e não UDP?**  
Em um sistema empresarial, garantia de entrega, ordem e confiabilidade são requisitos. O TCP oferece tudo isso nativamente. UDP seria adequado apenas para streams de vídeo/áudio ou jogos onde perder um pacote é aceitável — não é o caso aqui.

**Por que JSON por linha?**  
Simples de implementar, legível, sem biblioteca extra de serialização binária, e compatível com qualquer linguagem no cliente.

---

## Arquitetura

```
Cliente TCP
    │
    │  JSON (uma linha por mensagem, terminada em \n)
    │
    ▼
TcpServer  (ServerSocket na porta 7777)
    │
    ├── aceita conexão → cria ClientHandler em thread do pool
    │
ClientHandler  (uma thread por cliente)
    │
    ├── lê linha → parseia Request
    ├── resolve ClientSession (via SessionStore)
    │
    ▼
RequestDispatcher
    │
    ├── verifica autenticação (token)
    └── roteia action → ActionHandler
            │
            ├── AuthHandler       (LOGIN, LOGOUT)
            ├── UsuarioHandler    (CRUD de usuários)
            ├── MensagemHandler   (conversas, mensagens, PUSH)
            ├── ProdutoHandler    (CRUD + estoque)
            ├── CaixaHandler      (caixa, movimentações)
            ├── TarefaHandler     (CRUD de tarefas)
            ├── EquipeHandler     (CRUD de equipes)
            ├── CargoHandler      (cargos e permissões)
            ├── PontoHandler      (registro de ponto)
            └── LogHandler        (listagem de logs)
                    │
                    ▼
              Services / Repositories / JPA (já existentes)
                    │
                    ▼
              PostgreSQL
```

**Push de mensagens:** quando `MensagemHandler` processa `MENSAGEM_SEND`, além de persistir no banco, consulta o `SessionStore` e envia `PUSH_MENSAGEM` diretamente no socket de cada participante online — sem polling.

---

## Dependências

Adicione ao `pom.xml` do servidor (e do cliente, se usar Java):

```xml
<!-- JSON serialization -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

As demais dependências (JPA, EclipseLink, PostgreSQL driver) já existem no projeto.

---

## Configuração e Inicialização

O servidor lê as seguintes variáveis de ambiente (todas opcionais, com defaults):

| Variável          | Default | Descrição                                      |
|-------------------|---------|------------------------------------------------|
| `SERVER_PORT`     | `7777`  | Porta TCP do servidor                          |
| `MAX_CLIENTS`     | `100`   | Tamanho do thread pool (máximo de clientes simultâneos) |
| `JDBC_URL`        | —       | URL JDBC (sobrescreve persistence.xml)         |
| `JDBC_USER`       | —       | Usuário do banco                               |
| `JDBC_PASSWORD`   | —       | Senha do banco                                 |
| `JDBC_DRIVER`     | —       | Driver JDBC                                    |
| `HIBERNATE_HBM2DDL_AUTO` | — | Estratégia DDL do Hibernate                |
| `HIBERNATE_SHOW_SQL`     | — | Logar SQL gerado (`true`/`false`)          |

Para iniciar o servidor, basta executar `Main.java`. O servidor escuta indefinidamente até receber SIGTERM ou CTRL+C, quando faz shutdown gracioso (aguarda threads ativas por até 10 segundos).

---

## Protocolo de Comunicação

### Formato da Requisição (Cliente → Servidor)

```json
{
  "action":  "NOME_DA_ACTION",
  "token":   "uuid-da-sessao",
  "payload": {
    "campo1": "valor1",
    "campo2": 123
  }
}
```

- `action` — obrigatório, identifica a operação desejada.
- `token` — obrigatório em todas as actions exceto `LOGIN`.
- `payload` — campos dependem da action; pode ser omitido quando vazio.

### Formato da Resposta (Servidor → Cliente)

```json
{
  "status":  "OK",
  "action":  "NOME_DA_ACTION",
  "message": "Descrição legível.",
  "data":    { ... }
}
```

- `status` — `"OK"` ou `"ERROR"`.
- `action` — ecoa a action da requisição (ou `"PUSH_MENSAGEM"` para notificações).
- `message` — texto descritivo (útil para exibir erros ao usuário).
- `data` — payload da resposta; `null` em caso de erro ou operações sem retorno.

### Framing

Cada mensagem JSON ocupa **exatamente uma linha** (`\n`). O cliente e o servidor usam `BufferedReader.readLine()` para delimitar mensagens. Nunca quebre um JSON em múltiplas linhas.

---

## Autenticação

O fluxo de autenticação é:

```
Cliente                          Servidor
  │                                 │
  │── { action: "LOGIN",            │
  │    payload: {                   │
  │      email: "...",              │
  │      senha: "..."               │
  │    }                            │
  │  } ──────────────────────────► │
  │                                 │  valida email + senha
  │                                 │  cria ClientSession com UsuarioEntity
  │                                 │  registra no SessionStore
  │ ◄── { status: "OK",             │
  │       data: {                   │
  │         token: "uuid",          │
  │         usuarioId: 1,           │
  │         nome: "...",            │
  │         email: "..."            │
  │       }                         │
  │     } ──────────────────────── │
  │                                 │
  │  (salva o token localmente)     │
  │                                 │
  │── { action: "USUARIO_LIST",     │
  │    token: "uuid",               │
  │    payload: {}                  │
  │  } ──────────────────────────► │
  │                                 │  verifica token no SessionStore
  │ ◄── { status: "OK", data: [...] }
```

Todas as requisições após o LOGIN devem incluir o `token` retornado. Requisições sem token ou com token inválido recebem:

```json
{
  "status":  "ERROR",
  "action":  "NOME_DA_ACTION",
  "message": "Não autenticado. Faça LOGIN primeiro."
}
```

---

## Referência de Actions

### Autenticação

#### `LOGIN`
```json
// Requisição
{ "action": "LOGIN", "payload": { "email": "user@empresa.com", "senha": "123456" } }

// Resposta OK
{ "status": "OK", "action": "LOGIN", "data": { "token": "uuid", "usuarioId": 1, "nome": "João", "email": "user@empresa.com" } }
```

#### `LOGOUT`
```json
// Requisição
{ "action": "LOGOUT", "token": "uuid", "payload": {} }

// Resposta OK
{ "status": "OK", "action": "LOGOUT", "message": "Logout realizado." }
```

---

### Usuário

| Action            | Payload obrigatório                          | Payload opcional        |
|-------------------|----------------------------------------------|-------------------------|
| `USUARIO_CREATE`  | `nome`, `email`, `senha`                     | —                       |
| `USUARIO_LIST`    | —                                            | —                       |
| `USUARIO_GET`     | `id`                                         | —                       |
| `USUARIO_UPDATE`  | `id`                                         | `nome`, `email`, `senha`, `ativo` |
| `USUARIO_DELETE`  | `id`                                         | —                       |

```json
// USUARIO_CREATE
{ "action": "USUARIO_CREATE", "token": "...", "payload": { "nome": "Ana", "email": "ana@emp.com", "senha": "abc123" } }

// USUARIO_UPDATE
{ "action": "USUARIO_UPDATE", "token": "...", "payload": { "id": 3, "nome": "Ana Silva", "ativo": true } }
```

**Resposta de lista** (`USUARIO_LIST`):
```json
{ "status": "OK", "action": "USUARIO_LIST", "data": [
  { "id": 1, "nome": "João", "email": "joao@emp.com", "ativo": true },
  { "id": 2, "nome": "Ana",  "email": "ana@emp.com",  "ativo": true }
]}
```

---

### Produto e Estoque

| Action                  | Payload obrigatório                          | Payload opcional  |
|-------------------------|----------------------------------------------|-------------------|
| `PRODUTO_CREATE`        | `nome`, `precoUnitario`                      | `descricao`       |
| `PRODUTO_LIST`          | —                                            | —                 |
| `PRODUTO_GET`           | `id`                                         | —                 |
| `PRODUTO_UPDATE`        | `id`                                         | `nome`, `descricao`, `precoUnitario`, `quantidade` |
| `PRODUTO_DELETE`        | `id`                                         | —                 |
| `ESTOQUE_MOVIMENTAR`    | `produtoId`, `tipo` (`ENTRADA`\|`SAIDA`), `quantidade` | — |
| `ESTOQUE_MOVIMENTACOES` | `produtoId`                                  | —                 |

```json
// ESTOQUE_MOVIMENTAR
{ "action": "ESTOQUE_MOVIMENTAR", "token": "...", "payload": {
    "produtoId": 5, "tipo": "ENTRADA", "quantidade": 100
}}

// Resposta
{ "status": "OK", "data": { "movimentacaoId": 12, "estoqueAtual": 150 } }
```

---

### Caixa

| Action               | Payload obrigatório                              | Payload opcional  |
|----------------------|--------------------------------------------------|-------------------|
| `CAIXA_ABRIR`        | —                                                | `saldoInicial`    |
| `CAIXA_FECHAR`       | `id`                                             | `saldoFinal`      |
| `CAIXA_GET`          | `id`                                             | —                 |
| `CAIXA_MOVIMENTAR`   | `caixaId`, `tipo` (`ENTRADA`\|`SAIDA`), `valor`  | `descricao`       |
| `CAIXA_MOVIMENTACOES`| `caixaId`                                        | —                 |

```json
// CAIXA_ABRIR
{ "action": "CAIXA_ABRIR", "token": "...", "payload": { "saldoInicial": 500.00 } }

// CAIXA_MOVIMENTAR
{ "action": "CAIXA_MOVIMENTAR", "token": "...", "payload": {
    "caixaId": 1, "tipo": "ENTRADA", "valor": 150.00, "descricao": "Venda balcão"
}}
```

---

### Tarefa

| Action           | Payload obrigatório                                         | Payload opcional       |
|------------------|-------------------------------------------------------------|------------------------|
| `TAREFA_CREATE`  | `titulo`, `status`, `prioridade`, `dataInicio` (ISO 8601)  | `descricao`            |
| `TAREFA_LIST`    | —                                                           | —                      |
| `TAREFA_GET`     | `id`                                                        | —                      |
| `TAREFA_UPDATE`  | `id`                                                        | `titulo`, `status`, `prioridade`, `descricao`, `dataFim` |
| `TAREFA_DELETE`  | `id`                                                        | —                      |

```json
// TAREFA_CREATE
{ "action": "TAREFA_CREATE", "token": "...", "payload": {
    "titulo": "Revisar relatório Q3",
    "status": "ABERTA",
    "prioridade": "ALTA",
    "dataInicio": "2024-10-01T08:00:00"
}}
```

Valores sugeridos para `status`: `ABERTA`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`.  
Valores sugeridos para `prioridade`: `BAIXA`, `MEDIA`, `ALTA`, `CRITICA`.

---

### Equipe

| Action           | Payload obrigatório | Payload opcional |
|------------------|---------------------|------------------|
| `EQUIPE_CREATE`  | `nome`              | —                |
| `EQUIPE_LIST`    | —                   | —                |
| `EQUIPE_GET`     | `id`                | —                |
| `EQUIPE_UPDATE`  | `id`                | `nome`           |
| `EQUIPE_DELETE`  | `id`                | —                |

---

### Cargo e Permissão

| Action             | Payload obrigatório | Payload opcional |
|--------------------|---------------------|------------------|
| `CARGO_CREATE`     | `nome`              | —                |
| `CARGO_LIST`       | —                   | —                |
| `CARGO_GET`        | `id`                | —                |
| `CARGO_DELETE`     | `id`                | —                |
| `PERMISSAO_CREATE` | `nome`              | —                |
| `PERMISSAO_LIST`   | —                   | —                |

---

### Registro de Ponto

| Action                    | Payload obrigatório | Payload opcional |
|---------------------------|---------------------|------------------|
| `PONTO_REGISTRAR_ENTRADA` | —                   | —                |
| `PONTO_REGISTRAR_SAIDA`   | —                   | —                |
| `PONTO_LIST`              | —                   | `usuarioId`      |

Entrada e saída usam a data/hora do servidor no momento da chamada. `PONTO_LIST` sem `usuarioId` lista os registros do próprio usuário autenticado.

```json
// PONTO_LIST de outro usuário (requer permissão de gestão)
{ "action": "PONTO_LIST", "token": "...", "payload": { "usuarioId": 5 } }
```

---

### Mensagens e Conversas

| Action            | Payload obrigatório                          | Payload opcional |
|-------------------|----------------------------------------------|------------------|
| `CONVERSA_CREATE` | `tipo`, `participanteIds` (array de IDs)     | —                |
| `CONVERSA_LIST`   | —                                            | —                |
| `MENSAGEM_SEND`   | `conversaId`, `conteudo`                     | —                |
| `MENSAGEM_LIST`   | `conversaId`                                 | —                |

Valores de `tipo`: `PRIVADO` (1 para 1), `GRUPO`, `BROADCAST`.

```json
// CONVERSA_CREATE — grupo com os usuários 2 e 3 (o próprio usuário é incluído automaticamente)
{ "action": "CONVERSA_CREATE", "token": "...", "payload": {
    "tipo": "GRUPO",
    "participanteIds": [2, 3]
}}

// MENSAGEM_SEND
{ "action": "MENSAGEM_SEND", "token": "...", "payload": {
    "conversaId": 1,
    "conteudo": "Bom dia, equipe!"
}}
```

---

### Log do Sistema

| Action     | Payload obrigatório | Payload opcional          |
|------------|---------------------|---------------------------|
| `LOG_LIST` | —                   | `usuarioId`, `limit` (default 100) |

```json
{ "action": "LOG_LIST", "token": "...", "payload": { "usuarioId": 1, "limit": 50 } }
```

---

## Push em Tempo Real

Quando um usuário envia uma mensagem via `MENSAGEM_SEND`, o servidor **empurra automaticamente** uma notificação para todos os participantes da conversa que estiverem online, **sem que eles precisem fazer nenhuma requisição**.

### Formato do Push

```json
{
  "status":  "OK",
  "action":  "PUSH_MENSAGEM",
  "message": "Notificação em tempo real.",
  "data": {
    "mensagemId":  42,
    "conversaId":  1,
    "remetenteId": 3,
    "remetente":   "João",
    "conteudo":    "Bom dia, equipe!",
    "enviadoEm":   "2024-10-01T09:15:30"
  }
}
```

### Como tratar no cliente

O cliente precisa de **duas threads**:

1. **Thread principal** — envia requisições e lê respostas síncronas.
2. **Thread de escuta (push listener)** — fica em loop lendo linhas do socket e identificando pushes pelo campo `"action": "PUSH_MENSAGEM"`.

Exemplo em Java:

```java
// Thread de escuta — inicie após o LOGIN
new Thread(() -> {
    String line;
    while ((line = reader.readLine()) != null) {
        Map msg = gson.fromJson(line, Map.class);
        if ("PUSH_MENSAGEM".equals(msg.get("action"))) {
            Map data = (Map) msg.get("data");
            // Atualiza UI, dispara notificação, etc.
            System.out.printf("Nova mensagem de %s: %s%n",
                data.get("remetente"), data.get("conteudo"));
        }
    }
}).start();
```

> **Atenção:** em implementações mais robustas, use uma fila (ex: `LinkedBlockingQueue`) para correlacionar respostas às requisições e separar os pushes, evitando condições de corrida.

---

## Descrição dos Arquivos

### `protocol/`

| Arquivo              | Responsabilidade                                                         |
|----------------------|--------------------------------------------------------------------------|
| `Actions.java`       | Constantes de todas as actions suportadas (ex: `"USUARIO_CREATE"`).      |
| `request/Request.java` | POJO de desserialização da requisição. Métodos utilitários `getString()`, `getInteger()`. |
| `response/Response.java` | POJO de serialização da resposta. Fábricas estáticas `ok()`, `error()`, `push()`. |

### `network/`

| Arquivo                  | Responsabilidade                                                      |
|--------------------------|-----------------------------------------------------------------------|
| `TcpServer.java`         | Abre `ServerSocket`, aceita conexões, submete `ClientHandler` ao pool.|
| `ClientHandler.java`     | Thread de uma conexão. Lê linhas, parseia, despacha, envia resposta.  |
| `RequestDispatcher.java` | Verifica autenticação e roteia action para o handler correto.         |

### `network/session/`

| Arquivo              | Responsabilidade                                                         |
|----------------------|--------------------------------------------------------------------------|
| `ClientSession.java` | Encapsula `Socket` + `PrintWriter` + `UsuarioEntity` + `token`. Método `send()` thread-safe. |
| `SessionStore.java`  | Singleton. Mapeia `token → ClientSession` e `usuarioId → ClientSession`. Usado para push direto. |

### `network/handler/`

| Arquivo                | Actions tratadas                                                   |
|------------------------|--------------------------------------------------------------------|
| `AuthHandler.java`     | `LOGIN`, `LOGOUT`                                                  |
| `UsuarioHandler.java`  | `USUARIO_CREATE/LIST/GET/UPDATE/DELETE`                            |
| `MensagemHandler.java` | `CONVERSA_CREATE/LIST`, `MENSAGEM_SEND/LIST` + push               |
| `ProdutoHandler.java`  | `PRODUTO_CREATE/LIST/GET/UPDATE/DELETE`, `ESTOQUE_MOVIMENTAR/MOVIMENTACOES` |
| `CaixaHandler.java`    | `CAIXA_ABRIR/FECHAR/GET/MOVIMENTAR/MOVIMENTACOES`                 |
| `TarefaHandler.java`   | `TAREFA_CREATE/LIST/GET/UPDATE/DELETE`                             |
| `EquipeHandler.java`   | `EQUIPE_CREATE/LIST/GET/UPDATE/DELETE`                             |
| `CargoHandler.java`    | `CARGO_CREATE/LIST/GET/DELETE`, `PERMISSAO_CREATE/LIST`            |
| `PontoHandler.java`    | `PONTO_REGISTRAR_ENTRADA/SAIDA/LIST`                               |
| `LogHandler.java`      | `LOG_LIST`                                                         |

### `util/`

| Arquivo          | Responsabilidade                                                          |
|------------------|---------------------------------------------------------------------------|
| `JsonUtil.java`  | Singleton `Gson` com adaptadores para `LocalDateTime` e `LocalDate` (ISO 8601). |

### `client/`

| Arquivo          | Responsabilidade                                                          |
|------------------|---------------------------------------------------------------------------|
| `TcpClient.java` | Exemplo de cliente Java. Demonstra conexão, login, CRUD e escuta de pushes. Sirva como referência para implementar o client real. |

---

## Segurança — Próximos Passos

Os itens abaixo **não estão implementados** e devem ser adicionados antes de ir para produção:

**Hashing de senha**  
O `AuthHandler` atualmente compara a senha em texto puro. Substitua por:
```java
// Na criação do usuário:
String hash = BCrypt.hashpw(senha, BCrypt.gensalt());

// Na validação do login:
BCrypt.checkpw(senhaRecebida, usuario.getSenhaHash());
```
Adicione a dependência `org.mindrot:jbcrypt:0.4` ao `pom.xml`.

**JWT no lugar de UUID como token**  
O UUID atual não expira. Com JWT é possível definir tempo de expiração e assinar o token com uma chave secreta, invalidando tokens antigos automaticamente.

**TLS/SSL**  
Envolva o `ServerSocket` em `SSLServerSocket` para criptografar o tráfego em produção.

**Autorização por permissão**  
O `RequestDispatcher` verifica apenas autenticação (token válido). Para controle de acesso por cargo/permissão, consulte `usuario.getCargos()` nos handlers e verifique as permissões necessárias antes de executar a operação.

**Rate limiting**  
Adicione um contador de requisições por IP/sessão para prevenir abuso.