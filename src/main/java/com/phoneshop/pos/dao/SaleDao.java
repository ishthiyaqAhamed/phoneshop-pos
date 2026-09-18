package com.phoneshop.pos.dao;

import com.phoneshop.pos.config.DatabaseConfig;
import com.phoneshop.pos.model.Sale;
import com.phoneshop.pos.model.SaleItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SaleDao {
    private static final Logger logger = LoggerFactory.getLogger(SaleDao.class);
    private final ProductDao productDao = new ProductDao();

    public boolean processSale(Sale sale, int currentSessionId) {
        String insertSaleSql = "INSERT INTO sales (invoice_number, user_id, cashier_name, customer_id, customer_name, customer_phone, subtotal, discount_amount, tax_amount, total_amount, paid_amount, change_amount, payment_method, payment_status, notes, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        String insertItemSql = "INSERT INTO sale_items (sale_id, product_id, product_name, brand, category, imei, unit_cost, unit_price, quantity, discount_amount, subtotal, warranty_period) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String updateSessionSql = "UPDATE user_sessions SET shift_sales_total = shift_sales_total + ?, shift_orders_count = shift_orders_count + 1, last_active_time = CURRENT_TIMESTAMP WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // Begin ACID transaction

            // 1. Insert Sale record
            int saleId;
            try (PreparedStatement psSale = conn.prepareStatement(insertSaleSql, Statement.RETURN_GENERATED_KEYS)) {
                psSale.setString(1, sale.getInvoiceNumber());
                psSale.setInt(2, sale.getUserId());
                psSale.setString(3, sale.getCashierName());
                if (sale.getCustomerId() != null) {
                    psSale.setInt(4, sale.getCustomerId());
                } else {
                    psSale.setNull(4, Types.INTEGER);
                }
                psSale.setString(5, sale.getCustomerName());
                psSale.setString(6, sale.getCustomerPhone());
                psSale.setDouble(7, sale.getSubtotal());
                psSale.setDouble(8, sale.getDiscountAmount());
                psSale.setDouble(9, sale.getTaxAmount());
                psSale.setDouble(10, sale.getTotalAmount());
                psSale.setDouble(11, sale.getPaidAmount());
                psSale.setDouble(12, sale.getChangeAmount());
                psSale.setString(13, sale.getPaymentMethod());
                psSale.setString(14, sale.getPaymentStatus());
                psSale.setString(15, sale.getNotes());

                int affected = psSale.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Failed to create sale header.");
                }

                try (ResultSet rs = psSale.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleId = rs.getInt(1);
                        sale.setId(saleId);
                    } else {
                        throw new SQLException("Failed to retrieve generated sale ID.");
                    }
                }
            }

            // 2. Insert Sale Items and Deduct Stock
            try (PreparedStatement psItem = conn.prepareStatement(insertItemSql)) {
                for (SaleItem item : sale.getItems()) {
                    psItem.setInt(1, saleId);
                    if (item.getProductId() > 0) {
                        psItem.setInt(2, item.getProductId());
                        // Deduct stock
                        productDao.decrementStockInTransaction(conn, item.getProductId(), item.getQuantity());
                    } else {
                        psItem.setNull(2, Types.INTEGER);
                    }
                    psItem.setString(3, item.getProductName());
                    psItem.setString(4, item.getBrand());
                    psItem.setString(5, item.getCategory());
                    psItem.setString(6, item.getImei());
                    psItem.setDouble(7, item.getUnitCost());
                    psItem.setDouble(8, item.getUnitPrice());
                    psItem.setInt(9, item.getQuantity());
                    psItem.setDouble(10, item.getDiscountAmount());
                    psItem.setDouble(11, item.getSubtotal());
                    psItem.setString(12, item.getWarrantyPeriod());
                    psItem.addBatch();
                }
                psItem.executeBatch();
            }

            // 3. Update current cashier session shift stats
            if (currentSessionId > 0) {
                try (PreparedStatement psSession = conn.prepareStatement(updateSessionSql)) {
                    psSession.setDouble(1, sale.getTotalAmount());
                    psSession.setInt(2, currentSessionId);
                    psSession.executeUpdate();
                }
            }

            conn.commit(); // Commit transaction atomically
            logger.info("Sale {} processed successfully with {} items.", sale.getInvoiceNumber(), sale.getItems().size());
            return true;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("Transaction rolled back for sale: {}", sale.getInvoiceNumber());
                } catch (SQLException ex) {
                    logger.error("Failed to rollback transaction", ex);
                }
            }
            logger.error("Error processing sale {}: {}", sale.getInvoiceNumber(), e.getMessage(), e);
            throw new RuntimeException("Sale failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }

    public Sale findById(int saleId) {
        String sql = "SELECT * FROM sales WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sale sale = mapSale(rs);
                    sale.setItems(findItemsForSale(conn, saleId));
                    return sale;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding sale by id {}", saleId, e);
        }
        return null;
    }

    public Sale findByInvoiceNumber(String invoiceNumber) {
        String sql = "SELECT * FROM sales WHERE invoice_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sale sale = mapSale(rs);
                    sale.setItems(findItemsForSale(conn, sale.getId()));
                    return sale;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding sale by invoice number {}", invoiceNumber, e);
        }
        return null;
    }

    public List<Sale> findRecentSales(int limit, String searchKeyword, LocalDate startDate, LocalDate endDate) {
        List<Sale> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM sales WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (searchKeyword != null && !searchKeyword.isBlank()) {
            sql.append(" AND (LOWER(invoice_number) LIKE ? OR LOWER(customer_name) LIKE ? OR customer_phone LIKE ? OR LOWER(cashier_name) LIKE ?)");
            String pattern = "%" + searchKeyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (startDate != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.valueOf(startDate.atStartOfDay()));
        }

        if (endDate != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
        }

        sql.append(" ORDER BY created_at DESC LIMIT ?");
        params.add(limit);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sale s = mapSale(rs);
                    s.setItems(findItemsForSale(conn, s.getId()));
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching recent sales", e);
        }
        return list;
    }

    public List<SaleItem> findItemsForSale(Connection conn, int saleId) throws SQLException {
        List<SaleItem> items = new ArrayList<>();
        String sql = "SELECT * FROM sale_items WHERE sale_id = ? ORDER BY id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem item = new SaleItem();
                    item.setId(rs.getInt("id"));
                    item.setSaleId(rs.getInt("sale_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setBrand(rs.getString("brand"));
                    item.setCategory(rs.getString("category"));
                    item.setImei(rs.getString("imei"));
                    item.setUnitCost(rs.getDouble("unit_cost"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setDiscountAmount(rs.getDouble("discount_amount"));
                    item.setSubtotal(rs.getDouble("subtotal"));
                    item.setWarrantyPeriod(rs.getString("warranty_period"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public String generateNextInvoiceNumber() {
        String prefix = "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        String sql = "SELECT COUNT(*) FROM sales WHERE invoice_number LIKE ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1) + 1;
                    return String.format("%s%04d", prefix, count);
                }
            }
        } catch (SQLException e) {
            logger.error("Error generating invoice number", e);
        }
        return prefix + System.currentTimeMillis() % 10000;
    }

    private Sale mapSale(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("id"));
        s.setInvoiceNumber(rs.getString("invoice_number"));
        s.setUserId(rs.getInt("user_id"));
        s.setCashierName(rs.getString("cashier_name"));
        
        int custId = rs.getInt("customer_id");
        if (!rs.wasNull()) s.setCustomerId(custId);

        s.setCustomerName(rs.getString("customer_name"));
        s.setCustomerPhone(rs.getString("customer_phone"));
        s.setSubtotal(rs.getDouble("subtotal"));
        s.setDiscountAmount(rs.getDouble("discount_amount"));
        s.setTaxAmount(rs.getDouble("tax_amount"));
        s.setTotalAmount(rs.getDouble("total_amount"));
        s.setPaidAmount(rs.getDouble("paid_amount"));
        s.setChangeAmount(rs.getDouble("change_amount"));
        s.setPaymentMethod(rs.getString("payment_method"));
        s.setPaymentStatus(rs.getString("payment_status"));
        s.setNotes(rs.getString("notes"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) s.setCreatedAt(ts.toLocalDateTime());
        return s;
    }
}
