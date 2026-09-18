package com.phoneshop.pos.dao;

import com.phoneshop.pos.config.DatabaseConfig;
import com.phoneshop.pos.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserSessionDao {
    private static final Logger logger = LoggerFactory.getLogger(UserSessionDao.class);

    public int createSession(int userId, String username, String fullName) {
        String sql = "INSERT INTO user_sessions (user_id, username, full_name, login_time, last_active_time, status, shift_sales_total, shift_orders_count) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE', 0.00, 0)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, fullName);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating user session for user: {}", username, e);
        }
        return -1;
    }

    public void updateHeartbeat(int sessionId) {
        String sql = "UPDATE user_sessions SET last_active_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warn("Could not update session heartbeat for session {}: {}", sessionId, e.getMessage());
        }
    }

    public void addSaleToSession(int sessionId, double amount) {
        String sql = "UPDATE user_sessions SET shift_sales_total = shift_sales_total + ?, shift_orders_count = shift_orders_count + 1, last_active_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding sale to session {}", sessionId, e);
        }
    }

    public void endSession(int sessionId) {
        String sql = "UPDATE user_sessions SET logout_time = CURRENT_TIMESTAMP, status = 'LOGGED_OUT' WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error ending user session {}", sessionId, e);
        }
    }

    public List<UserSession> findCurrentlyActiveSessions() {
        List<UserSession> list = new ArrayList<>();
        String sql = "SELECT * FROM user_sessions WHERE status = 'ACTIVE' ORDER BY login_time DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapSession(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching active user sessions", e);
        }
        return list;
    }

    public List<UserSession> findAllSessions(int limit) {
        List<UserSession> list = new ArrayList<>();
        String sql = "SELECT * FROM user_sessions ORDER BY login_time DESC LIMIT " + limit;
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapSession(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all user sessions", e);
        }
        return list;
    }

    private UserSession mapSession(ResultSet rs) throws SQLException {
        UserSession s = new UserSession();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setUsername(rs.getString("username"));
        s.setFullName(rs.getString("full_name"));
        
        Timestamp loginTs = rs.getTimestamp("login_time");
        if (loginTs != null) s.setLoginTime(loginTs.toLocalDateTime());

        Timestamp logoutTs = rs.getTimestamp("logout_time");
        if (logoutTs != null) s.setLogoutTime(logoutTs.toLocalDateTime());

        Timestamp lastActiveTs = rs.getTimestamp("last_active_time");
        if (lastActiveTs != null) s.setLastActiveTime(lastActiveTs.toLocalDateTime());

        s.setStatus(rs.getString("status"));
        s.setShiftSalesTotal(rs.getDouble("shift_sales_total"));
        s.setShiftOrdersCount(rs.getInt("shift_orders_count"));
        return s;
    }
}
