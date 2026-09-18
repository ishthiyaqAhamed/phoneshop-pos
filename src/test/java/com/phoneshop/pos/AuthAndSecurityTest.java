package com.phoneshop.pos;

import com.phoneshop.pos.config.DatabaseConfig;
import com.phoneshop.pos.dao.UserDao;
import com.phoneshop.pos.dao.UserSessionDao;
import com.phoneshop.pos.model.Role;
import com.phoneshop.pos.model.User;
import com.phoneshop.pos.service.AuthService;
import com.phoneshop.pos.util.AppSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuthAndSecurityTest {

    @BeforeAll
    public static void setup() {
        DatabaseConfig.initialize();
    }

    @Test
    public void testDefaultUsersSeeded() {
        UserDao userDao = new UserDao();
        List<User> users = userDao.findAll();
        assertFalse(users.isEmpty(), "Users table should have seeded users");

        User admin = userDao.findByUsername("admin");
        assertNotNull(admin, "Admin user should exist");
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(BCrypt.checkpw("admin123", admin.getPasswordHash()), "Admin password should match admin123");

        User cashier1 = userDao.findByUsername("cashier1");
        assertNotNull(cashier1, "Cashier 1 should exist");
        assertEquals(Role.CASHIER, cashier1.getRole());
        assertTrue(BCrypt.checkpw("cashier123", cashier1.getPasswordHash()), "Cashier1 password should match cashier123");
    }

    @Test
    public void testAuthServiceLoginAndLogout() {
        AuthService authService = new AuthService();

        // Valid login
        boolean ok = authService.login("cashier1", "cashier123");
        assertTrue(ok, "Cashier 1 login should succeed");
        assertTrue(AppSession.getInstance().isLoggedIn());
        assertTrue(AppSession.getInstance().isCashier());
        assertFalse(AppSession.getInstance().isAdmin());

        int sessionId = AppSession.getInstance().getCurrentSessionId();
        assertTrue(sessionId > 0, "Session ID should be generated");

        // Logout
        authService.logout();
        assertFalse(AppSession.getInstance().isLoggedIn());

        // Invalid login
        boolean badPass = authService.login("cashier1", "wrongpassword");
        assertFalse(badPass, "Invalid password login should fail");
    }
}
