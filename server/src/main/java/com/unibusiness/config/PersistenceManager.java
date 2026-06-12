package com.unibusiness.config;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
public final class PersistenceManager {
    private static final EntityManagerFactory ENTITY_MANAGER_FACTORY = buildEntityManagerFactory();
    private PersistenceManager() {}
    private static EntityManagerFactory buildEntityManagerFactory() {
        Map<String, String> props = new HashMap<>();
        String url = System.getenv("JDBC_URL");
        String user = System.getenv("JDBC_USER");
        String password = System.getenv("JDBC_PASSWORD");
        String driver = System.getenv("JDBC_DRIVER");
        String ddl = System.getenv("HIBERNATE_HBM2DDL_AUTO");
        String showSql = System.getenv("HIBERNATE_SHOW_SQL");
        if (url != null && !url.isBlank()) props.put("javax.persistence.jdbc.url", url);
        if (user != null && !user.isBlank()) props.put("javax.persistence.jdbc.user", user);
        if (password != null && !password.isBlank()) props.put("javax.persistence.jdbc.password", password);
        if (driver != null && !driver.isBlank()) props.put("javax.persistence.jdbc.driver", driver);
        if (ddl != null && !ddl.isBlank()) props.put("hibernate.hbm2ddl.auto", ddl);
        if (showSql != null && !showSql.isBlank()) props.put("hibernate.show_sql", showSql);
        return Persistence.createEntityManagerFactory("uni-business", props);
    }
    public static EntityManagerFactory getEntityManagerFactory() { return ENTITY_MANAGER_FACTORY; }
    public static void close() { if (ENTITY_MANAGER_FACTORY.isOpen()) ENTITY_MANAGER_FACTORY.close(); }
}