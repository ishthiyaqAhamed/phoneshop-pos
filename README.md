# Phone Shop POS — Desktop Point of Sale System

A desktop Point of Sale (POS) and Inventory Management System designed for phone retail shops. Built with Java (JDK 17), JavaFX, Neon Cloud PostgreSQL, and ZXing Barcode Engine.

---

## Key Features

### 1. Role-Based Access Control (RBAC)
- **Admin User**:
  - Full access to POS Terminal to make sales.
  - Add, edit, and delete products (Phone specs, IMEI, colors, warranty terms, prices).
  - Generate and print barcode price tags.
  - View **Sales History & Invoices** with receipt re-printing.
  - View **Stock-Level & Inventory Valuation Dashboard** with low-stock alerts.
  - View **Profit & Revenue Analytics** (Gross profit margins, COGS, top-selling phones & accessories).
  - Monitor **Cashier Login & Shift Activity** in real time.
  - Manage Staff Accounts & reset passwords.
- **Cashier Users (Cashier 1 & Cashier 2)**:
  - Access to POS Terminal to perform sales checkout.
  - Add & edit products in inventory.
  - Generate & print barcode labels.
  - *Restricted from*: Viewing sales history, profit/revenue reports, stock valuation dashboard, and cashier login logs.

### 2. Phone Shop Specific Product Management
- Categorized for **Smartphones, Tablets, Accessories, Spare Parts, Services**.
- Detailed attributes:
  - Brand (Apple, Samsung, Xiaomi, etc.)
  - Model & Specs (Storage, RAM, Color, Condition: Brand New, Pre-Owned, Refurbished)
  - IMEI / Serial Number tracking
  - Configurable **Warranty Period** per product (e.g. `1 Year Official Apple Care`, `6 Months Shop Warranty`, `1 Month Testing Warranty`, `No Warranty`)
  - Cost (Buying Price) vs Selling (Retail Price)
  - Stock count with customizable low-stock threshold.

### 3. ZXing Barcode Generation & Printing
- Generates **Code 128 / EAN 13** barcodes.
- Generates printable **Price & Warranty Labels (50mm x 35mm)** showing:
  - Shop Name & Branch
  - Product Model & Specs (Storage / Color)
  - Barcode & SKU Number
  - Warranty Badge & Selling Price
- Supports direct label printing (`javax.print`) and PNG image export.

### 4. POS Terminal & Checkout
- **Instant Barcode / IMEI scanner input** with auto-focus and Enter key trigger.
- Category filter chips (Smartphones, Accessories, Tablets, Spare Parts).
- Customer phone number lookup / auto-registration for warranty tracking.
- Itemized cart with quantity adjustment, warranty badge display, and discount calculations.
- Multiple payment options: **Cash**, **Card / POS Terminal**, **Bank Transfer / QR**, **Split**.
- Real-time change calculation with quick-cash tender buttons.
- Atomic transaction checkout: updates stock and records cashier shift sales.
- **80mm Thermal Receipt Generator** and **A5 PDF Invoice Exporter** with itemized IMEI and warranty policy terms.

### 5. Cashier Shift & Login Activity Monitoring
- Live status of active cashiers currently logged into the system.
- Historical session logs recording login time, logout time, shift duration, total sales generated, and transaction counts.

---

## Default Seed Accounts

When running the application for the first time, default accounts are seeded:

| Role | Username | Default Password | Full Name |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | System Administrator |
| **Cashier 1** | `cashier1` | `cashier123` | Cashier Sarah |
| **Cashier 2** | `cashier2` | `cashier123` | Cashier David |

> Passwords are encrypted and hashed with salted BCrypt.

---

## Configuration (`.env`)

Edit the `.env` file in the project root to configure your database and shop details:

```env
# Neon Cloud PostgreSQL Connection
DB_URL=jdbc:postgresql://<your-neon-endpoint>.neon.tech/neondb?sslmode=require
DB_USER=<your-neon-username>
DB_PASSWORD=<your-neon-password>

# Shop Branding (Printed on Invoices & Labels)
SHOP_NAME=APEX PHONE & ACCESSORIES
SHOP_BRANCH=Main City Flagship Store
SHOP_ADDRESS=120 Galle Road, Colombo 03
SHOP_PHONE=+94 77 123 4567 / +94 11 234 5678
SHOP_EMAIL=support@apexphoneshop.com
SHOP_TAX_ID=VAT-99887766-001

# Regional Settings
CURRENCY_SYMBOL=Rs.
CURRENCY_CODE=LKR
TAX_RATE_PERCENT=0.0
```

> **Offline Fallback**: If `DB_URL` is set to `jdbc:sqlite:pos_offline.db`, the application will use the embedded local SQLite database, allowing testing and offline operation.

---

## Running the Application

### Using Maven:
```powershell
mvn clean compile javafx:run
```

### Or using Maven Wrapper / direct path:
```powershell
& "C:\Program Files\NetBeans-25\netbeans\java\maven\bin\mvn.cmd" javafx:run
```

### 1-Click Launch Options:
- **Desktop Shortcut**: Double-click the **`Phone Shop POS`** shortcut on your Windows Desktop.
- **Batch Launcher**: Double-click [`Launch-POS.bat`](file:///c:/Users/Dell/Desktop/phoneshop-pos/Launch-POS.bat) in the root directory.
- **Native Standalone Executable**:
  ```powershell
  .\dist\PhoneShopPOS\PhoneShopPOS.exe
  ```
  *(Runs completely standalone with its own bundled runtime — no Java installation required on the client machine!)*

### Rebuilding Standalone Package:
```powershell
.\build-dist.ps1
```

### Creating / Re-creating Desktop Shortcut:
```powershell
.\Create-Desktop-Shortcut.ps1
```

---

## Technical Stack
- **Java**: OpenJDK 17 (LTS)
- **UI Framework**: JavaFX 21 (Modern Dark POS Glassmorphism Theme)
- **Database**: Neon Cloud PostgreSQL / SQLite offline fallback via HikariCP connection pool
- **Security**: Salted BCrypt password hashing & role-based access control (Admin / Cashier)
- **Barcode & Labeling**: ZXing Barcode Engine (Code-128 & EAN-13) + Java AWT Label Tag Rendering
- **Invoicing & Receipts**: OpenPDF (iText) A5 invoice generation & 80mm ESC/POS Thermal Receipt formatting

