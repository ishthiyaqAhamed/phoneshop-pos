package com.phoneshop.pos.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    public static synchronized void initialize() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        String jdbcUrl = AppConfig.getDbUrl();
        String username = AppConfig.getDbUser();
        String password = AppConfig.getDbPassword();

        logger.info("Initializing database connection with URL: {}", jdbcUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);

        boolean isPostgres = jdbcUrl.startsWith("jdbc:postgresql:");
        boolean isSqlite = jdbcUrl.startsWith("jdbc:sqlite:");

        if (isPostgres) {
            config.setDriverClassName("org.postgresql.Driver");
            if (username != null && !username.isBlank()) {
                config.setUsername(username);
            }
            if (password != null && !password.isBlank()) {
                config.setPassword(password);
            }
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(300000); // 5 minutes
            config.setMaxLifetime(600000); // 10 minutes (good for Neon serverless)
            config.setConnectionTimeout(20000); // 20 seconds
            config.setPoolName("NeonPostgresPool");
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslmode", "require");
        } else if (isSqlite) {
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(5);
            config.setPoolName("SqliteOfflinePool");
        } else {
            if (username != null && !username.isBlank()) {
                config.setUsername(username);
            }
            if (password != null && !password.isBlank()) {
                config.setPassword(password);
            }
            config.setMaximumPoolSize(10);
        }

        try {
            dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized successfully.");
            
            // Run schema initialization and migration
            SchemaInitializer.initializeSchema();
        } catch (Exception e) {
            logger.error("Failed to initialize database connection: {}", e.getMessage(), e);
            throw new RuntimeException("Database connection error: " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initialize();
        }
        return dataSource.getConnection();
    }

    public static boolean isPostgres() {
        return AppConfig.getDbUrl().startsWith("jdbc:postgresql:");
    }

    public static synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed.");
        }
    }
}
