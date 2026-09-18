package com.phoneshop.pos.config;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SchemaInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SchemaInitializer.class);

    public static void initializeSchema() {
        boolean isPg = DatabaseConfig.isPostgres();
        String autoInc = isPg ? "SERIAL PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        String timestampType = isPg ? "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" : "DATETIME DEFAULT CURRENT_TIMESTAMP";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id " + autoInc + ", " +
                    "username VARCHAR(50) UNIQUE NOT NULL, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL, " +
                    "is_active BOOLEAN DEFAULT TRUE, " +
                    "created_at " + timestampType + ")");

            // 2. User Sessions (Cashier activity tracking)
            stmt.execute("CREATE TABLE IF NOT EXISTS user_sessions (" +
                    "id " + autoInc + ", " +
                    "user_id INTEGER NOT NULL, " +
                    "username VARCHAR(50) NOT NULL, " +
                    "full_name VARCHAR(100) NOT NULL, " +
                    "login_time " + timestampType + ", " +
                    "logout_time " + (isPg ? "TIMESTAMP WITH TIME ZONE" : "DATETIME") + ", " +
                    "last_active_time " + timestampType + ", " +
                    "status VARCHAR(20) DEFAULT 'ACTIVE', " +
                    "shift_sales_total DECIMAL(12, 2) DEFAULT 0.00, " +
                    "shift_orders_count INTEGER DEFAULT 0, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");

            // 3. Products table
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id " + autoInc + ", " +
                    "barcode VARCHAR(100) UNIQUE NOT NULL, " +
                    "imei VARCHAR(100), " +
                    "name VARCHAR(150) NOT NULL, " +
                    "brand VARCHAR(50) NOT NULL, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "storage_ram VARCHAR(50), " +
                    "color VARCHAR(50), " +
                    "condition VARCHAR(30) DEFAULT 'BRAND_NEW', " +
                    "cost_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00, " +
                    "selling_price DECIMAL(12, 2) NOT NULL, " +
                    "stock_quantity INTEGER NOT NULL DEFAULT 0, " +
                    "min_stock_level INTEGER NOT NULL DEFAULT 3, " +
                    "warranty_period VARCHAR(100) NOT NULL DEFAULT 'No Warranty', " +
                    "description TEXT, " +
                    "updated_at " + timestampType + ")");

            // 4. Customers table
            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "id " + autoInc + ", " +
                    "name VARCHAR(100) NOT NULL, " +
                    "phone VARCHAR(30) UNIQUE NOT NULL, " +
                    "email VARCHAR(100), " +
                    "address TEXT, " +
                    "created_at " + timestampType + ")");

            // 5. Sales (Invoices) table
            stmt.execute("CREATE TABLE IF NOT EXISTS sales (" +
                    "id " + autoInc + ", " +
                    "invoice_number VARCHAR(50) UNIQUE NOT NULL, " +
                    "user_id INTEGER NOT NULL, " +
                    "cashier_name VARCHAR(100) NOT NULL, " +
                    "customer_id INTEGER, " +
                    "customer_name VARCHAR(100), " +
                    "customer_phone VARCHAR(30), " +
                    "subtotal DECIMAL(12, 2) NOT NULL, " +
                    "discount_amount DECIMAL(12, 2) DEFAULT 0.00, " +
                    "tax_amount DECIMAL(12, 2) DEFAULT 0.00, " +
                    "total_amount DECIMAL(12, 2) NOT NULL, " +
                    "paid_amount DECIMAL(12, 2) NOT NULL, " +
                    "change_amount DECIMAL(12, 2) DEFAULT 0.00, " +
                    "payment_method VARCHAR(30) NOT NULL, " +
                    "payment_status VARCHAR(20) DEFAULT 'PAID', " +
                    "notes TEXT, " +
                    "created_at " + timestampType + ", " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

            // 6. Sale Items table
            stmt.execute("CREATE TABLE IF NOT EXISTS sale_items (" +
                    "id " + autoInc + ", " +
                    "sale_id INTEGER NOT NULL, " +
                    "product_id INTEGER, " +
                    "product_name VARCHAR(150) NOT NULL, " +
                    "brand VARCHAR(50), " +
                    "category VARCHAR(50), " +
                    "imei VARCHAR(100), " +
                    "unit_cost DECIMAL(12, 2) NOT NULL DEFAULT 0.00, " +
                    "unit_price DECIMAL(12, 2) NOT NULL, " +
                    "quantity INTEGER NOT NULL, " +
                    "discount_amount DECIMAL(12, 2) DEFAULT 0.00, " +
                    "subtotal DECIMAL(12, 2) NOT NULL, " +
                    "warranty_period VARCHAR(100), " +
                    "FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE)");

            // 7. Shop settings key-value store
            stmt.execute("CREATE TABLE IF NOT EXISTS shop_settings (" +
                    "id " + autoInc + ", " +
                    "setting_key VARCHAR(50) UNIQUE NOT NULL, " +
                    "setting_value TEXT NOT NULL)");

            logger.info("Database tables verified/created successfully.");

            // Seed initial data if empty
            seedInitialUsers(conn);
            seedInitialProducts(conn);

        } catch (Exception e) {
            logger.error("Error creating database schema: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static void seedInitialUsers(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                logger.info("No users found. Seeding 1 Admin and 2 Cashier accounts...");

                String insertSql = "INSERT INTO users (username, password_hash, full_name, role, is_active) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    // Admin: admin / admin123
                    ps.setString(1, "admin");
                    ps.setString(2, BCrypt.hashpw("admin123", BCrypt.gensalt(10)));
                    ps.setString(3, "Admin Manager");
                    ps.setString(4, "ADMIN");
                    ps.setBoolean(5, true);
                    ps.executeUpdate();

                    // Cashier 1: cashier1 / cashier123
                    ps.setString(1, "cashier1");
                    ps.setString(2, BCrypt.hashpw("cashier123", BCrypt.gensalt(10)));
                    ps.setString(3, "Cashier Sarah");
                    ps.setString(4, "CASHIER");
                    ps.setBoolean(5, true);
                    ps.executeUpdate();

                    // Cashier 2: cashier2 / cashier123
                    ps.setString(1, "cashier2");
                    ps.setString(2, BCrypt.hashpw("cashier123", BCrypt.gensalt(10)));
                    ps.setString(3, "Cashier David");
                    ps.setString(4, "CASHIER");
                    ps.setBoolean(5, true);
                    ps.executeUpdate();

                    logger.info("Default users seeded: admin (admin123), cashier1 (cashier123), cashier2 (cashier123)");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to seed initial users: {}", e.getMessage(), e);
        }
    }

    private static void seedInitialProducts(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products")) {
            if (rs.next() && rs.getInt(1) == 0) {
                logger.info("No products found. Seeding initial phone shop inventory...");

                String insertSql = "INSERT INTO products (barcode, imei, name, brand, category, storage_ram, color, condition, cost_price, selling_price, stock_quantity, min_stock_level, warranty_period, description) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    // Item 1: iPhone 15 Pro Max
                    ps.setString(1, "195949012345");
                    ps.setString(2, "354892110293847");
                    ps.setString(3, "iPhone 15 Pro Max");
                    ps.setString(4, "Apple");
                    ps.setString(5, "SMARTPHONES");
                    ps.setString(6, "256GB / 8GB RAM");
                    ps.setString(7, "Natural Titanium");
                    ps.setString(8, "BRAND_NEW");
                    ps.setDouble(9, 325000.00);
                    ps.setDouble(10, 375000.00);
                    ps.setInt(11, 8);
                    ps.setInt(12, 2);
                    ps.setString(13, "1 Year Apple Care");
                    ps.setString(14, "Official TRCSL approved brand new sealed box");
                    ps.executeUpdate();

                    // Item 2: Samsung Galaxy S24 Ultra
                    ps.setString(1, "880609654321");
                    ps.setString(2, "356781229384751");
                    ps.setString(3, "Samsung Galaxy S24 Ultra");
                    ps.setString(4, "Samsung");
                    ps.setString(5, "SMARTPHONES");
                    ps.setString(6, "512GB / 12GB RAM");
                    ps.setString(7, "Titanium Gray");
                    ps.setString(8, "BRAND_NEW");
                    ps.setDouble(9, 295000.00);
                    ps.setDouble(10, 345000.00);
                    ps.setInt(11, 5);
                    ps.setInt(12, 2);
                    ps.setString(13, "1 Year Company Warranty");
                    ps.setString(14, "Samsung Gen-AI flagship smartphone");
                    ps.executeUpdate();

                    // Item 3: Xiaomi Redmi Note 13 Pro
                    ps.setString(1, "693417771234");
                    ps.setString(2, "867542039485712");
                    ps.setString(3, "Redmi Note 13 Pro");
                    ps.setString(4, "Xiaomi");
                    ps.setString(5, "SMARTPHONES");
                    ps.setString(6, "256GB / 8GB RAM");
                    ps.setString(7, "Midnight Black");
                    ps.setString(8, "BRAND_NEW");
                    ps.setDouble(9, 68000.00);
                    ps.setDouble(10, 82000.00);
                    ps.setInt(11, 15);
                    ps.setInt(12, 4);
                    ps.setString(13, "1 Year Company Warranty");
                    ps.setString(14, "200MP Camera, 67W Turbo Charge");
                    ps.executeUpdate();

                    // Item 4: Baseus 65W GaN Fast Charger
                    ps.setString(1, "695315620111");
                    ps.setString(2, "N/A");
                    ps.setString(3, "Baseus 65W GaN5 Pro Fast Charger");
                    ps.setString(4, "Baseus");
                    ps.setString(5, "ACCESSORIES");
                    ps.setString(6, "65W 3-Port Type-C+USB");
                    ps.setString(7, "Black");
                    ps.setString(8, "BRAND_NEW");
                    ps.setDouble(9, 4500.00);
                    ps.setDouble(10, 7500.00);
                    ps.setInt(11, 30);
                    ps.setInt(12, 5);
                    ps.setString(13, "6 Months Shop Warranty");
                    ps.setString(14, "Supports iPhone, Samsung, MacBook fast charging");
                    ps.executeUpdate();

                    // Item 5: Anker Soundcore R50i Earbuds
                    ps.setString(1, "194644021234");
                    ps.setString(2, "SN-ANK99281");
                    ps.setString(3, "Anker Soundcore R50i TWS Earbuds");
                    ps.setString(4, "Anker");
                    ps.setString(5, "ACCESSORIES");
                    ps.setString(6, "Bluetooth 5.3 / 30H Playtime");
                    ps.setString(7, "Black");
                    ps.setString(8, "BRAND_NEW");
                    ps.setDouble(9, 5200.00);
                    ps.setDouble(10, 8500.00);
                    ps.setInt(11, 20);
                    ps.setInt(12, 5);
                    ps.setString(13, "6 Months Warranty");
                    ps.setString(14, "IPX5 water resistant, 10mm drivers with extra bass");
                    ps.executeUpdate();

                    // Item 6: iPhone 13 OLED Display Spare Part
                    ps.setString(1, "990011223344");
                    ps.setString(2, "DISP-IP13-88");
                    ps.setString(3, "iPhone 13 Original OLED Display");
                    ps.setString(4, "Apple Parts");
                    ps.setString(5, "SPARE_PARTS");
                    ps.setString(6, "Super Retina XDR Replacement");
                    ps.setString(7, "Black");
                    ps.setString(8, "BRAND_NEW");
                    ps.setDouble(9, 17500.00);
                    ps.setDouble(10, 26000.00);
                    ps.setInt(11, 3);
                    ps.setInt(12, 2);
                    ps.setString(13, "1 Month Testing Warranty");
                    ps.setString(14, "High grade OLED with TrueTone support");
                    ps.executeUpdate();

                    logger.info("Seeded initial phone shop product catalog.");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to seed initial products: {}", e.getMessage(), e);
        }
    }
}
