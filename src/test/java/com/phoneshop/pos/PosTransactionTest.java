package com.phoneshop.pos;

import com.phoneshop.pos.config.DatabaseConfig;
import com.phoneshop.pos.dao.ProductDao;
import com.phoneshop.pos.dao.SaleDao;
import com.phoneshop.pos.dao.UserSessionDao;
import com.phoneshop.pos.model.Product;
import com.phoneshop.pos.model.Sale;
import com.phoneshop.pos.model.SaleItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PosTransactionTest {

    @BeforeAll
    public static void setup() {
        DatabaseConfig.initialize();
    }

    @Test
    public void testCheckoutTransactionAndStockDeduction() {
        ProductDao productDao = new ProductDao();
        SaleDao saleDao = new SaleDao();
        UserSessionDao sessionDao = new UserSessionDao();

        List<Product> products = productDao.findAll();
        assertFalse(products.isEmpty(), "Should have seeded products");

        Product testProd = products.get(0);
        int initialStock = testProd.getStockQuantity();
        assertTrue(initialStock >= 2, "Test product must have sufficient initial stock");

        int sessionId = sessionDao.createSession(1, "cashier1", "Cashier Sarah");

        // Create sale for 2 items
        Sale sale = new Sale();
        String invoiceNum = saleDao.generateNextInvoiceNumber();
        sale.setInvoiceNumber(invoiceNum);
        sale.setUserId(1);
        sale.setCashierName("Cashier Sarah");
        sale.setCustomerName("Nimal Perera");
        sale.setCustomerPhone("0771234567");

        SaleItem item = new SaleItem(testProd, 2);
        sale.addItem(item);
        sale.setSubtotal(item.getSubtotal());
        sale.setTotalAmount(item.getSubtotal());
        sale.setPaidAmount(item.getSubtotal());
        sale.setChangeAmount(0.0);
        sale.setPaymentMethod("CASH");

        boolean success = saleDao.processSale(sale, sessionId);
        assertTrue(success, "Sale transaction should execute successfully");

        // Verify stock deducted
        Product updatedProd = productDao.findById(testProd.getId());
        assertEquals(initialStock - 2, updatedProd.getStockQuantity(), "Stock should be decremented by 2");

        // Verify sale record & items
        Sale retrieved = saleDao.findByInvoiceNumber(invoiceNum);
        assertNotNull(retrieved, "Sale should be retrievable by invoice number");
        assertEquals(1, retrieved.getItems().size());
        assertEquals(item.getWarrantyPeriod(), retrieved.getItems().get(0).getWarrantyPeriod());

        sessionDao.endSession(sessionId);
    }
}
