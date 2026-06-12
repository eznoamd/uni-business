package com.unibusiness.config;
import com.unibusiness.model.CargoEntity;
import com.unibusiness.model.PermissaoEntity;
import com.unibusiness.model.UsuarioEntity;
import com.unibusiness.util.PasswordUtil;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.Set;
public class DatabasePopulate {
    public static void run() {
        EntityManager em = PersistenceManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Long count = em.createQuery("SELECT count(u) FROM UsuarioEntity u", Long.class).getSingleResult();
            if (count > 0) { System.out.println("[Populate] Banco já possui registros. Pulando inicialização."); tx.commit(); return; }
            System.out.println("[Populate] Iniciando carga de dados padrão...");
            PermissaoEntity permAdmin = new PermissaoEntity("ADMIN_TOTAL");
            PermissaoEntity permUser = new PermissaoEntity("USER_READ");
            em.persist(permAdmin); em.persist(permUser);
            CargoEntity cargoGerente = new CargoEntity("Gerente");
            cargoGerente.setPermissoes(Set.of(permAdmin, permUser));
            CargoEntity cargoOperador = new CargoEntity("Operador");
            cargoOperador.setPermissoes(Set.of(permUser));
            em.persist(cargoGerente); em.persist(cargoOperador);
            String hash = PasswordUtil.hashPassword("asasas");
            UsuarioEntity admin = new UsuarioEntity("Enzo Augusto", "a@a", hash);
            admin.setCargos(Set.of(cargoGerente));
            UsuarioEntity example1 = new UsuarioEntity("Example1", "e@e", hash);
            example1.setCargos(Set.of(cargoOperador));
            em.persist(admin); em.persist(example1);
            tx.commit();
            System.out.println("[Populate] Dados iniciais carregados com sucesso!");
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            System.err.println("[Populate] Erro ao popular banco: " + e.getMessage());
            e.printStackTrace();
        } finally { em.close(); }
    }
}