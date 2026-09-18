package com.phoneshop.pos.service;

import com.phoneshop.pos.dao.UserDao;
import com.phoneshop.pos.dao.UserSessionDao;
import com.phoneshop.pos.model.User;
import com.phoneshop.pos.util.AppSession;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDao userDao = new UserDao();
    private final UserSessionDao sessionDao = new UserSessionDao();

    public boolean login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }

        User user = userDao.findByUsername(username);
        if (user == null) {
            logger.warn("Login attempt for non-existent user: {}", username);
            return false;
        }

        if (!user.isActive()) {
            logger.warn("Login attempt for deactivated user: {}", username);
            return false;
        }

        boolean passwordMatch = false;
        try {
            passwordMatch = BCrypt.checkpw(password, user.getPasswordHash());
        } catch (Exception e) {
            logger.error("Error checking password hash for user {}: {}", username, e.getMessage());
        }

        if (passwordMatch) {
            int sessionId = sessionDao.createSession(user.getId(), user.getUsername(), user.getFullName());
            AppSession.getInstance().setCurrentUser(user, sessionId);
            logger.info("User {} ({}) logged in successfully. Session ID: {}", user.getUsername(), user.getRole(), sessionId);
            return true;
        } else {
            logger.warn("Invalid password attempt for user: {}", username);
            return false;
        }
    }

    public void logout() {
        int sessionId = AppSession.getInstance().getCurrentSessionId();
        if (sessionId > 0) {
            sessionDao.endSession(sessionId);
        }
        User user = AppSession.getInstance().getCurrentUser();
        if (user != null) {
            logger.info("User {} logged out. Session ID: {}", user.getUsername(), sessionId);
        }
        AppSession.getInstance().clear();
    }
}
