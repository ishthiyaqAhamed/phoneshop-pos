# Phone Shop POS - Standalone Distribution Builder
# Builds both the Fat JAR (pos-1.0.0.jar) and the Standalone Windows Native Application (PhoneShopPOS.exe)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Phone Shop POS - Standalone Application Builder" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

$mavenPath = "C:\Program Files\NetBeans-25\netbeans\java\maven\bin\mvn.cmd"
$jpackagePath = "C:\Program Files\Java\jdk-17\bin\jpackage.exe"

if (-not (Test-Path $mavenPath)) {
    Write-Host "Error: Maven not found at '$mavenPath'" -ForegroundColor Red
    exit 1
}

Write-Host "[1/3] Compiling and Packaging Uber Fat JAR with Maven..." -ForegroundColor Green
& $mavenPath clean package -DskipTests

if (-not (Test-Path "target\pos-1.0.0.jar")) {
    Write-Host "Error: Fat JAR build failed." -ForegroundColor Red
    exit 1
}

Write-Host "[2/3] Fat JAR successfully created: target\pos-1.0.0.jar" -ForegroundColor Green

if (Test-Path $jpackagePath) {
    Write-Host "[3/3] Generating Native Windows Executable (.exe) with jpackage..." -ForegroundColor Green
    if (Test-Path "dist") {
        Remove-Item -Recurse -Force "dist"
    }
    & $jpackagePath `
        --type app-image `
        --input target `
        --name "PhoneShopPOS" `
        --main-jar pos-1.0.0.jar `
        --main-class com.phoneshop.pos.MainLauncher `
        --dest dist `
        --vendor "Apex Phone Shop" `
        --app-version "1.0.0" `
        --description "Modern Point of Sale & Inventory System for Phone Shops"

    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host " Standalone Native App created at: dist\PhoneShopPOS\PhoneShopPOS.exe" -ForegroundColor Yellow
    Write-Host " (Runs independently on any Windows PC without needing Java!)" -ForegroundColor Gray
    Write-Host "==========================================================" -ForegroundColor Cyan
} else {
    Write-Host "Note: jpackage not found, Fat JAR is ready to run with 'java -jar target\pos-1.0.0.jar'" -ForegroundColor Yellow
}
