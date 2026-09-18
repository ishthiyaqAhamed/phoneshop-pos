package com.phoneshop.pos.dao;

import com.phoneshop.pos.config.DatabaseConfig;
import com.phoneshop.pos.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CustomerDao {
    private static final Logger logger = LoggerFactory.getLogger(CustomerDao.class);

    public Customer findByPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String sql = "SELECT * FROM customers WHERE phone = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapCustomer(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding customer by phone {}", phone, e);
        }
        return null;
    }

    public Customer findById(int id) {
        String sql = "SELECT * FROM customers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapCustomer(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding customer by id {}", id, e);
        }
        return null;
    }

    public List<Customer> search(String query) {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE LOWER(name) LIKE ? OR phone LIKE ? ORDER BY name ASC LIMIT 20";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + (query == null ? "" : query.trim().toLowerCase()) + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCustomer(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching customers {}", query, e);
        }
        return list;
    }

    public Customer getOrCreate(String name, String phone, String email, String address) {
        Customer existing = findByPhone(phone);
        if (existing != null) {
            return existing;
        }
        Customer newCust = new Customer();
        newCust.setName(name == null || name.isBlank() ? "Walk-in Customer" : name.trim());
        newCust.setPhone(phone.trim());
        newCust.setEmail(email);
        newCust.setAddress(address);
        create(newCust);
        return newCust;
    }

    public boolean create(Customer c) {
        String sql = "INSERT INTO customers (name, phone, email, address, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getAddress());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        c.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error creating customer: {}", c.getName(), e);
        }
        return false;
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        return c;
    }
}
