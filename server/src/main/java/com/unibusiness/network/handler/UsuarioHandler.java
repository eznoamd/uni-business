package com.unibusiness.network.handler;

import com.unibusiness.config.PersistenceManager;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.network.session.ClientSession;
import com.unibusiness.protocol.Actions;
import com.unibusiness.protocol.request.Request;
import com.unibusiness.protocol.response.Response;
import com.unibusiness.repository.UsuarioRepository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles: USUARIO_CREATE, USUARIO_LIST, USUARIO_GET, USUARIO_UPDATE, USUARIO_DELETE
 */
public class UsuarioHandler implements ActionHandler {

    @Override
    public Response handle(Request req, ClientSession session) {
        return switch (req.getAction()) {
            case Actions.USUARIO_CREATE -> create(req);
            case Actions.USUARIO_LIST   -> list();
            case Actions.USUARIO_GET    -> get(req);
            case Actions.USUARIO_UPDATE -> update(req);
            case Actions.USUARIO_DELETE -> delete(req);
            default -> Response.error(req.getAction(), "Action não suportada por UsuarioHandler.");
        };
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    private Response create(Request req) {
        String nome  = req.getString("nome");
        String email = req.getString("email");
        String senha = req.getString("senha");

        if (nome == null || email == null || senha == null) {
            return Response.error(Actions.USUARIO_CREATE, "Campos 'nome', 'email' e 'senha' obrigatórios.");
        }

        // NOTA: use BCrypt.hashpw(senha, BCrypt.gensalt()) em produção
        UsuarioEntity u = new UsuarioEntity(nome, email, senha);

        EntityManager em = em();
        try {
            UsuarioEntity saved = new UsuarioRepository(em).save(u);
            return Response.ok(Actions.USUARIO_CREATE, toMap(saved));
        } finally { em.close(); }
    }

    // ── LIST ──────────────────────────────────────────────────────────────────

    private Response list() {
        EntityManager em = em();
        try {
            List<Map<String, Object>> result = new UsuarioRepository(em)
                    .findAll().stream().map(this::toMap).collect(Collectors.toList());
            return Response.ok(Actions.USUARIO_LIST, result);
        } finally { em.close(); }
    }

    // ── GET ───────────────────────────────────────────────────────────────────

    private Response get(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.USUARIO_GET, "Campo 'id' obrigatório.");

        EntityManager em = em();
        try {
            UsuarioEntity u = new UsuarioRepository(em).findById(id);
            if (u == null) return Response.error(Actions.USUARIO_GET, "Usuário não encontrado.");
            return Response.ok(Actions.USUARIO_GET, toMap(u));
        } finally { em.close(); }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    private Response update(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.USUARIO_UPDATE, "Campo 'id' obrigatório.");

        EntityManager em = em();
        try {
            UsuarioRepository repo = new UsuarioRepository(em);
            UsuarioEntity u = repo.findById(id);
            if (u == null) return Response.error(Actions.USUARIO_UPDATE, "Usuário não encontrado.");

            if (req.getString("nome")  != null) u.setNome(req.getString("nome"));
            if (req.getString("email") != null) u.setEmail(req.getString("email"));
            if (req.getString("senha") != null) u.setSenhaHash(req.getString("senha"));
            if (req.get("ativo")       != null) u.setAtivo(Boolean.parseBoolean(req.getString("ativo")));

            UsuarioEntity saved = repo.save(u);
            return Response.ok(Actions.USUARIO_UPDATE, toMap(saved));
        } finally { em.close(); }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    private Response delete(Request req) {
        Integer id = req.getInteger("id");
        if (id == null) return Response.error(Actions.USUARIO_DELETE, "Campo 'id' obrigatório.");

        EntityManager em = em();
        try {
            UsuarioRepository repo = new UsuarioRepository(em);
            UsuarioEntity u = repo.findById(id);
            if (u == null) return Response.error(Actions.USUARIO_DELETE, "Usuário não encontrado.");
            repo.delete(u);
            return Response.ok(Actions.USUARIO_DELETE, "Usuário removido.", null);
        } finally { em.close(); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EntityManager em() {
        return PersistenceManager.getEntityManagerFactory().createEntityManager();
    }

    private Map<String, Object> toMap(UsuarioEntity u) {
        return Map.of(
            "id",    u.getId(),
            "nome",  u.getNome(),
            "email", u.getEmail(),
            "ativo", u.getAtivo()
        );
    }
}
