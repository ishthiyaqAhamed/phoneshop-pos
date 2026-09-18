package com.phoneshop.pos.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static Dotenv dotenv;

    static {
        try {
            dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
        } catch (Exception e) {
            logger.warn("Could not load .env file, falling back to system environment variables/defaults: {}", e.getMessage());
            dotenv = null;
        }
    }

    public static String get(String key, String defaultValue) {
        String val = null;
        if (dotenv != null) {
            val = dotenv.get(key);
        }
        if (val == null || val.isBlank()) {
            val = System.getenv(key);
        }
        if (val == null || val.isBlank()) {
            val = System.getProperty(key);
        }
        return (val != null && !val.isBlank()) ? val.trim() : defaultValue;
    }

    public static String getDbUrl() {
        return get("DB_URL", "jdbc:sqlite:pos_offline.db");
    }

    public static String getDbUser() {
        return get("DB_USER", "");
    }

    public static String getDbPassword() {
        return get("DB_PASSWORD", "");
    }

    public static String getShopName() {
        return get("SHOP_NAME", "APEX PHONE & ACCESSORIES");
    }

    public static String getShopBranch() {
        return get("SHOP_BRANCH", "Main Flagship Store");
    }

    public static String getShopAddress() {
        return get("SHOP_ADDRESS", "120 Galle Road, Colombo 03");
    }

    public static String getShopPhone() {
        return get("SHOP_PHONE", "+94 77 123 4567 / +94 11 234 5678");
    }

    public static String getShopEmail() {
        return get("SHOP_EMAIL", "support@apexphoneshop.com");
    }

    public static String getShopTaxId() {
        return get("SHOP_TAX_ID", "VAT-99887766-001");
    }

    public static String getCurrencySymbol() {
        return get("CURRENCY_SYMBOL", "Rs.");
    }

    public static String getCurrencyCode() {
        return get("CURRENCY_CODE", "LKR");
    }

    public static double getTaxRatePercent() {
        try {
            return Double.parseDouble(get("TAX_RATE_PERCENT", "0.0"));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static String getWarrantyTerms() {
        return get("SHOP_WARRANTY_TERMS", 
                "1. Warranty claims require original receipt and matching device IMEI/Serial. " +
                "2. Physical, liquid, or surge damage voids warranty. " +
                "3. Accessories carry a 30-day replacement warranty.");
    }
}
