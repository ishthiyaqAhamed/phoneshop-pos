package com.phoneshop.pos.dao;

import com.phoneshop.pos.config.DatabaseConfig;
import com.phoneshop.pos.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    private static final Logger logger = LoggerFactory.getLogger(ProductDao.class);

    public Product findById(int id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProduct(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding product by id {}", id, e);
        }
        return null;
    }

    public Product findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return null;
        String sql = "SELECT * FROM products WHERE barcode = ? OR imei = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode.trim());
            ps.setString(2, barcode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProduct(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding product by barcode/imei {}", barcode, e);
        }
        return null;
    }

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all products", e);
        }
        return list;
    }

    public List<Product> search(String keyword, String category) {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (category != null && !category.equalsIgnoreCase("ALL") && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category);
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(brand) LIKE ? OR LOWER(barcode) LIKE ? OR LOWER(imei) LIKE ? OR LOWER(storage_ram) LIKE ?)");
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append(" ORDER BY name ASC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProduct(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching products with keyword '{}' category '{}'", keyword, category, e);
        }
        return list;
    }

    public List<Product> findLowStockProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE stock_quantity <= min_stock_level ORDER BY stock_quantity ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding low stock products", e);
        }
        return list;
    }

    public boolean create(Product p) {
        String sql = "INSERT INTO products (barcode, imei, name, brand, category, storage_ram, color, condition, cost_price, selling_price, stock_quantity, min_stock_level, warranty_period, description, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getBarcode());
            ps.setString(2, p.getImei());
            ps.setString(3, p.getName());
            ps.setString(4, p.getBrand());
            ps.setString(5, p.getCategory());
            ps.setString(6, p.getStorageRam());
            ps.setString(7, p.getColor());
            ps.setString(8, p.getCondition());
            ps.setDouble(9, p.getCostPrice());
            ps.setDouble(10, p.getSellingPrice());
            ps.setInt(11, p.getStockQuantity());
            ps.setInt(12, p.getMinStockLevel());
            ps.setString(13, p.getWarrantyPeriod());
            ps.setString(14, p.getDescription());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error creating product: {}", p.getName(), e);
        }
        return false;
    }

    public boolean update(Product p) {
        String sql = "UPDATE products SET barcode = ?, imei = ?, name = ?, brand = ?, category = ?, storage_ram = ?, color = ?, " +
                "condition = ?, cost_price = ?, selling_price = ?, stock_quantity = ?, min_stock_level = ?, warranty_period = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getBarcode());
            ps.setString(2, p.getImei());
            ps.setString(3, p.getName());
            ps.setString(4, p.getBrand());
            ps.setString(5, p.getCategory());
            ps.setString(6, p.getStorageRam());
            ps.setString(7, p.getColor());
            ps.setString(8, p.getCondition());
            ps.setDouble(9, p.getCostPrice());
            ps.setDouble(10, p.getSellingPrice());
            ps.setInt(11, p.getStockQuantity());
            ps.setInt(12, p.getMinStockLevel());
            ps.setString(13, p.getWarrantyPeriod());
            ps.setString(14, p.getDescription());
            ps.setInt(15, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating product id: {}", p.getId(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting product id: {}", id, e);
        }
        return false;
    }

    public boolean updateStock(int productId, int delta) {
        String sql = "UPDATE products SET stock_quantity = stock_quantity + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating stock for productId: {}", productId, e);
        }
        return false;
    }

    public void decrementStockInTransaction(Connection conn, int productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND stock_quantity >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Insufficient stock or product not found for ID: " + productId);
            }
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setBarcode(rs.getString("barcode"));
        p.setImei(rs.getString("imei"));
        p.setName(rs.getString("name"));
        p.setBrand(rs.getString("brand"));
        p.setCategory(rs.getString("category"));
        p.setStorageRam(rs.getString("storage_ram"));
        p.setColor(rs.getString("color"));
        p.setCondition(rs.getString("condition"));
        p.setCostPrice(rs.getDouble("cost_price"));
        p.setSellingPrice(rs.getDouble("selling_price"));
        p.setStockQuantity(rs.getInt("stock_quantity"));
        p.setMinStockLevel(rs.getInt("min_stock_level"));
        p.setWarrantyPeriod(rs.getString("warranty_period"));
        p.setDescription(rs.getString("description"));

        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) p.setUpdatedAt(ts.toLocalDateTime());
        return p;
    }
}
