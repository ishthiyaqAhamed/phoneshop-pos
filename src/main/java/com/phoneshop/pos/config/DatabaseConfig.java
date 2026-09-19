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
        logger.info("Initializing SQLite database connection with URL: {}", jdbcUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(5);
        config.setPoolName("SqliteOfflinePool");

        try {
            dataSource = new HikariDataSource(config);
            logger.info("SQLite database connection pool initialized successfully.");
            
            // Run schema initialization
            SchemaInitializer.initializeSchema();
        } catch (Exception e) {
            logger.error("Failed to initialize SQLite database: {}", e.getMessage(), e);
            throw new RuntimeException("Database connection error: " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initialize();
        }
        return dataSource.getConnection();
    }

    public static synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("SQLite database connection pool closed.");
        }
    }
}
