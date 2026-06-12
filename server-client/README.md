# UniBusiness — JavaFX direto no banco (sem servidor TCP)

## Resumo da migração

Este projeto agora é **um único módulo JavaFX** que fala direto com o
Postgres via JPA/EclipseLink. O servidor TCP (sockets, JSON, dispatcher,
handlers, sessões, push) foi completamente removido.

```
Antes:  Tela JavaFX -> TcpClient -> Socket -> TcpServer -> Dispatcher
                     -> Handler -> Service -> Repository -> Banco

Agora:  Tela JavaFX -> Service (com.unibusiness.service.*)
                     -> Service "core" (com.unibusiness.core.service.*, JPA)
                     -> Repository -> Banco
```

## Estrutura do projeto

```
com.unibusiness
├── core/                  ← veio do servidor antigo, renomeado para core.*
│   ├── model/             entidades JPA (UsuarioEntity, ProdutoEntity, ...)
│   ├── repository/        GenericRepository + repositórios específicos
│   ├── service/(.impl)    regra de negócio (JPA puro, sem rede)
│   ├── config/            PersistenceManager, DatabasePopulate
│   └── util/PasswordUtil  hash de senha (bcrypt)
│
├── service/               FACHADAS usadas pelas telas (Dto-based)
│   ├── AuthService         login/logout (substitui token por UsuarioEntity)
│   ├── UsuarioService       (era TCP USUARIO_LIST)
│   ├── ProdutoService        (era TCP PRODUTO_*/ESTOQUE_*)
│   ├── EquipeService          (era TCP EQUIPE_*)
│   ├── TarefaService           (era TCP TAREFA_*)
│   ├── CaixaService              (era TCP CAIXA_*)
│   └── ConversaService            (era TCP CONVERSA_*/MENSAGEM_*)
│
├── session/SessionManager   guarda a UsuarioEntity logada (sem token)
├── dto/Dto                   DTOs usados pelas TableView/ListView das telas
├── controller/                controllers JavaFX (quase nenhum mudou!)
└── manager/                    ViewManager/ViewService (inalterados)
```

**O que NÃO mudou:** `core/model`, `core/repository`, `core/service(.impl)`,
`core/config` e `core/util` são exatamente a regra de negócio que já existia
no servidor — só trocaram de pacote (`com.unibusiness.X` →
`com.unibusiness.core.X`). A maioria dos controllers (`EstoqueController`,
`FinanceiroController`, `TarefasController`, `EquipesController`,
`FuncionariosController`, `HomeController`, `MinhasTarefasController`)
**não foi alterada** — eles continuam chamando
`com.unibusiness.service.XxxService` com os mesmos métodos de antes.

## O que mudou de fato

- **Removido**: `network/` (TcpClient, PushListener, PushRouter,
  PresenceCache), `dto/ServerResponse`, dependência `gson`, Dockerfile do
  client (não roda mais em container — é um app desktop).
- **`SessionManager`**: antes guardava `token + Dto.UsuarioLogado`; agora
  guarda a `UsuarioEntity` direto.
- **`AuthService`**: antes fazia `LOGIN`/`LOGOUT` via socket; agora consulta
  `UsuarioService.findByEmail()` + `PasswordUtil.checkPassword()` no banco.
- **`LoginController`**: removida a conexão `TcpClient.connect(host, port)`.
- **`Main.java`**: agora inicializa o `PersistenceManager`/`DatabasePopulate`
  ao abrir, e marca o usuário como offline ao fechar a janela.
- **As 7 fachadas em `service/`**: reescritas para chamar
  `core.service.impl.*` (JPA) em vez de `TcpClient.send(...)`, convertendo
  entidades para os mesmos `Dto.*` que as telas já usavam.

## Como ficou o "online das pessoas" (sua pergunta)

Sem servidor TCP não há mais conexão persistente para saber quem está
conectado em tempo real. A solução simples adotada:

1. **`UsuarioEntity`** ganhou dois campos novos: `online` (boolean) e
   `ultimoAcessoEm` (timestamp).
2. **No login** (`AuthService.login`), o usuário é marcado `online = true`.
3. **No logout ou ao fechar a janela** (`AuthService.logout`, chamado pelo
   `Main.stop`/`setOnCloseRequest`), é marcado `online = false`.
4. **Para ver o status de outras pessoas** (lista de conversas no Chat), o
   `ChatController` agora faz **polling**: a cada 4 segundos (`Timeline`),
   recarrega a lista de conversas (que já vem com `online` lido do banco) e,
   se uma conversa estiver aberta, recarrega as mensagens novas.

Ou seja: "online" deixou de ser tempo real via socket e passou a ser
"o outro usuário fez login e ainda não saiu, e eu confiro isso a cada poucos
segundos". Para o escopo de uma entrega de faculdade isso é suficiente e
muito mais simples que manter conexões/threads/push.

**Limitação conhecida**: se o app fechar de forma anormal (crash, força
encerrar processo), o `setOnCloseRequest` não roda e o usuário fica marcado
como "online" no banco para sempre. Se quiser mitigar isso sem complicar,
dá para tratar "online" como "online se `ultimoAcessoEm` foi há menos de
N segundos" — mas não implementei isso para manter simples; fica como
possível próximo passo se sobrar tempo.

## Como rodar

1. Subir o banco (só o Postgres):
   ```
   docker compose up -d
   ```
2. Rodar o app:
   ```
   mvn javafx:run
   ```
   (ou `make run`, que já existia no Makefile)

Na primeira execução, `DatabasePopulate` cria as tabelas e os usuários de
exemplo (`a@a` / senha `asasas`, `e@e` / senha `asasas`).

## Pendências / próximos passos sugeridos

- `MinhasTarefasController` ainda usa dados mock (`carregarDadosMock()`) —
  poderia ser ligado ao `TarefaService` da mesma forma que
  `TarefasController`.
- As abas "Clientes" e "Fornecedores" (referenciadas no `DashboardController`
  e nos FXMLs) não têm controller/service ainda — ficam fora do escopo desta
  migração.
