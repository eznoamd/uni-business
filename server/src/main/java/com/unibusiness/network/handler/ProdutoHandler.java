package com.unibusiness.network.handler;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.MovimentacaoEstoqueEntity;
import com.unibusiness.model.ProdutoEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.repository.GenericRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles: PRODUTO_CREATE, PRODUTO_LIST, PRODUTO_GET, PRODUTO_UPDATE,
 *          PRODUTO_DELETE, ESTOQUE_MOVIMENTAR, ESTOQUE_MOVIMENTACOES
 *
 * PRODUTO_CREATE    payload: { "nome": "...", "precoUnitario": 9.99, "descricao": "..." }
 * PRODUTO_UPDATE    payload: { "id": 1, "nome": "...", "precoUnitario": 9.99, "quantidade": 10 }
 * ESTOQUE_MOVIMENTAR payload: { "produtoId": 1, "tipo": "ENTRADA|SAIDA", "quantidade": 5 }
 * ESTOQUE_MOVIMENTACOES payload: { "produtoId": 1 }
 */
public class ProdutoHandler implements ActionHandler {

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.PRODUTO_CREATE        -> create(req);
            case Actions.PRODUTO_LIST          -> list();
            case Actions.PRODUTO_GET           -> get(req);
            case Actions.PRODUTO_UPDATE        -> update(req);
            case Actions.PRODUTO_DELETE        -> delete(req);
            case Actions.ESTOQUE_MOVIMENTAR    -> movimentar(req, session);
            case Actions.ESTOQUE_MOVIMENTACOES -> listarMovimentacoes(req);
            default -> Response.error(req.getAction(), "Action não suportada por ProdutoHandler.");
        };
    }

    private Response create(Request req) {
        String nome  = req.getString("nome");
        Number preco = (Number) req.get("precoUnitario");
        if (nome == null || preco == null)
            return Response.error(Actions.PRODUTO_CREATE, "Campos 'nome' e 'precoUnitario' obrigatórios.");

        ProdutoEntity p = new ProdutoEntity(nome, preco.floatValue());
        if (req.getString("descricao") != null) p.setDescricao(req.getString("descricao"));

        EntityManager em = em();
        try { return Response.ok(Actions.PRODUTO_CREATE, toMap(new GenericRepository<>(ProdutoEntity.class, em).save(p))); }
        finally { em.close(); }
    }

    private Response list() {
        EntityManager em = em();
        try {
            List<Map<String,Object>> l = new GenericRepository<>(ProdutoEntity.class, em)
                .findAll().stream().map(this::toMap).collect(Collectors.toList());
            return Response.ok(Actions.PRODUTO_LIST, l);
        } finally { em.close(); }
    }

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.PRODUTO_GET, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            ProdutoEntity p = new GenericRepository<>(ProdutoEntity.class, em).findById(id);
            if (p == null) return Response.error(Actions.PRODUTO_GET, "Produto não encontrado.");
            return Response.ok(Actions.PRODUTO_GET, toMap(p));
        } finally { em.close(); }
    }

    private Response update(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.PRODUTO_UPDATE, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            GenericRepository<ProdutoEntity> repo = new GenericRepository<>(ProdutoEntity.class, em);
            ProdutoEntity p = repo.findById(id);
            if (p == null) return Response.error(Actions.PRODUTO_UPDATE, "Produto não encontrado.");
            if (req.getString("nome") != null)       p.setNome(req.getString("nome"));
            if (req.getString("descricao") != null)  p.setDescricao(req.getString("descricao"));
            if (req.get("precoUnitario") != null)    p.setPrecoUnitario(((Number) req.get("precoUnitario")).floatValue());
            if (req.get("quantidade") != null)       p.setQuantidade(req.getInteger("quantidade"));
            return Response.ok(Actions.PRODUTO_UPDATE, toMap(repo.save(p)));
        } finally { em.close(); }
    }

    private Response delete(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.PRODUTO_DELETE, "Campo 'id' obrigatório.");
        EntityManager em = em();
        try {
            GenericRepository<ProdutoEntity> repo = new GenericRepository<>(ProdutoEntity.class, em);
            ProdutoEntity p = repo.findById(id);
            if (p == null) return Response.error(Actions.PRODUTO_DELETE, "Produto não encontrado.");
            repo.delete(p);
            return Response.ok(Actions.PRODUTO_DELETE, "Produto removido.", null);
        } finally { em.close(); }
    }

    private Response movimentar(Request req, ClientSession session) {
        Integer produtoId  = req.getInteger("produtoId");
        String  tipo       = req.getString("tipo");
        Integer quantidade = req.getInteger("quantidade");

        if (produtoId == null || tipo == null || quantidade == null)
            return Response.error(Actions.ESTOQUE_MOVIMENTAR, "Campos 'produtoId', 'tipo' e 'quantidade' obrigatórios.");

        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ProdutoEntity produto = em.find(ProdutoEntity.class, produtoId);
            if (produto == null) { tx.rollback(); return Response.error(Actions.ESTOQUE_MOVIMENTAR, "Produto não encontrado."); }

            UsuarioEntity usuario = em.find(UsuarioEntity.class, session.getUsuario().getId());

            if ("SAIDA".equalsIgnoreCase(tipo) && produto.getQuantidade() < quantidade)
                { tx.rollback(); return Response.error(Actions.ESTOQUE_MOVIMENTAR, "Estoque insuficiente."); }

            produto.setQuantidade(produto.getQuantidade() + ("ENTRADA".equalsIgnoreCase(tipo) ? quantidade : -quantidade));
            em.merge(produto);

            MovimentacaoEstoqueEntity mov = new MovimentacaoEstoqueEntity(produto, tipo.toUpperCase(), quantidade, usuario);
            if (req.getString("descricao") != null) {}  // campo não existe na entidade, extensível
            em.persist(mov);

            tx.commit();
            return Response.ok(Actions.ESTOQUE_MOVIMENTAR, Map.of(
                "movimentacaoId", mov.getId(),
                "estoqueAtual",   produto.getQuantidade()
            ));
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            return Response.error(Actions.ESTOQUE_MOVIMENTAR, "Erro: " + e.getMessage());
        } finally { em.close(); }
    }

    private Response listarMovimentacoes(Request req) {
        Integer produtoId = req.getInteger("produtoId");
        if (produtoId == null) return Response.error(Actions.ESTOQUE_MOVIMENTACOES, "Campo 'produtoId' obrigatório.");
        EntityManager em = em();
        try {
            List<Map<String,Object>> list = em.createQuery(
                "SELECT m FROM MovimentacaoEstoqueEntity m WHERE m.produto.id = :pid ORDER BY m.data",
                MovimentacaoEstoqueEntity.class)
                .setParameter("pid", produtoId)
                .getResultList()
                .stream()
                .map(m -> Map.<String,Object>of(
                    "id",         m.getId(),
                    "tipo",       m.getTipo(),
                    "quantidade", m.getQuantidade(),
                    "data",       m.getData().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ))
                .collect(Collectors.toList());
            return Response.ok(Actions.ESTOQUE_MOVIMENTACOES, list);
        } finally { em.close(); }
    }

    private EntityManager em() { return PersistenceManager.getEntityManagerFactory().createEntityManager(); }

    private Map<String, Object> toMap(ProdutoEntity p) {
        return Map.of(
            "id",            p.getId(),
            "nome",          p.getNome(),
            "descricao",     p.getDescricao() != null ? p.getDescricao() : "",
            "quantidade",    p.getQuantidade(),
            "precoUnitario", p.getPrecoUnitario()
        );
    }
}
