package com.phoneshop.pos.dao;

import com.phoneshop.pos.config.DatabaseConfig;
import com.phoneshop.pos.model.DashboardSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportDao {
    private static final Logger logger = LoggerFactory.getLogger(ReportDao.class);

    public DashboardSummary getDashboardSummary() {
        DashboardSummary summary = new DashboardSummary();
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        try (Connection conn = DatabaseConfig.getConnection()) {

            // 1. Today's Revenue & Orders Count
            String todaySql = "SELECT COALESCE(SUM(total_amount), 0.0), COUNT(*) FROM sales WHERE created_at >= ?";
            try (PreparedStatement ps = conn.prepareStatement(todaySql)) {
                ps.setTimestamp(1, Timestamp.valueOf(today.atStartOfDay()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        summary.setTodayRevenue(rs.getDouble(1));
                        summary.setTodayOrdersCount(rs.getInt(2));
                    }
                }
            }

            // 2. Today's Profit (Subtotal of items sold - Total cost of items sold)
            String todayProfitSql = "SELECT COALESCE(SUM(si.subtotal - (si.unit_cost * si.quantity)), 0.0) " +
                    "FROM sale_items si JOIN sales s ON si.sale_id = s.id WHERE s.created_at >= ?";
            try (PreparedStatement ps = conn.prepareStatement(todayProfitSql)) {
                ps.setTimestamp(1, Timestamp.valueOf(today.atStartOfDay()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        summary.setTodayProfit(rs.getDouble(1));
                    }
                }
            }

            // 3. Month's Revenue & Orders Count
            String monthSql = "SELECT COALESCE(SUM(total_amount), 0.0), COUNT(*) FROM sales WHERE created_at >= ?";
            try (PreparedStatement ps = conn.prepareStatement(monthSql)) {
                ps.setTimestamp(1, Timestamp.valueOf(startOfMonth.atStartOfDay()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        summary.setMonthRevenue(rs.getDouble(1));
                        summary.setMonthOrdersCount(rs.getInt(2));
                    }
                }
            }

            // 4. Month's Profit
            String monthProfitSql = "SELECT COALESCE(SUM(si.subtotal - (si.unit_cost * si.quantity)), 0.0) " +
                    "FROM sale_items si JOIN sales s ON si.sale_id = s.id WHERE s.created_at >= ?";
            try (PreparedStatement ps = conn.prepareStatement(monthProfitSql)) {
                ps.setTimestamp(1, Timestamp.valueOf(startOfMonth.atStartOfDay()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        summary.setMonthProfit(rs.getDouble(1));
                    }
                }
            }

            // 5. Stock inventory totals
            String stockSql = "SELECT COUNT(*), " +
                    "SUM(CASE WHEN stock_quantity <= min_stock_level AND stock_quantity > 0 THEN 1 ELSE 0 END), " +
                    "SUM(CASE WHEN stock_quantity <= 0 THEN 1 ELSE 0 END), " +
                    "COALESCE(SUM(selling_price * stock_quantity), 0.0), " +
                    "COALESCE(SUM(cost_price * stock_quantity), 0.0) " +
                    "FROM products";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(stockSql)) {
                if (rs.next()) {
                    summary.setTotalProductsCount(rs.getInt(1));
                    summary.setLowStockCount(rs.getInt(2));
                    summary.setOutOfStockCount(rs.getInt(3));
                    summary.setInventoryTotalValue(rs.getDouble(4));
                    summary.setInventoryTotalCost(rs.getDouble(5));
                }
            }

            // 6. Top selling products
            String topSql = "SELECT product_name, SUM(quantity) as total_qty FROM sale_items GROUP BY product_name ORDER BY total_qty DESC LIMIT 5";
            Map<String, Integer> topSelling = new LinkedHashMap<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(topSql)) {
                while (rs.next()) {
                    topSelling.put(rs.getString("product_name"), rs.getInt("total_qty"));
                }
            }
            summary.setTopSellingProducts(topSelling);

        } catch (SQLException e) {
            logger.error("Error generating dashboard summary", e);
        }

        return summary;
    }

    public Map<String, Double> getDailySalesForLastDays(int days) {
        Map<String, Double> map = new LinkedHashMap<>();
        LocalDate start = LocalDate.now().minusDays(days - 1);
        
        // Initialize days
        for (int i = 0; i < days; i++) {
            map.put(start.plusDays(i).toString(), 0.0);
        }

        String sql = "SELECT DATE(created_at) as sale_date, SUM(total_amount) as total " +
                "FROM sales WHERE created_at >= ? GROUP BY DATE(created_at) ORDER BY sale_date ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String d = rs.getString("sale_date");
                    if (d != null && d.length() >= 10) {
                        d = d.substring(0, 10);
                        map.put(d, rs.getDouble("total"));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching daily sales", e);
        }
        return map;
    }

    public Map<String, Double> getSalesByPaymentMethod() {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT payment_method, SUM(total_amount) as total FROM sales GROUP BY payment_method";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("payment_method"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            logger.error("Error fetching payment method distribution", e);
        }
        return map;
    }
}
