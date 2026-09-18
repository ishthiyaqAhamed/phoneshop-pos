@echo off
title Phone Shop POS System
cd /d "%~dp0"

if exist "dist\PhoneShopPOS\PhoneShopPOS.exe" (
    echo Starting Standalone Native Phone Shop POS...
    start "" "dist\PhoneShopPOS\PhoneShopPOS.exe"
    exit /b 0
)

if exist "target\pos-1.0.0.jar" (
    echo Starting Phone Shop POS from Fat JAR...
    start "" javaw -jar "target\pos-1.0.0.jar"
    exit /b 0
)

echo POS Executable or JAR not found. Building now with Maven...
call "C:\Program Files\NetBeans-25\netbeans\java\maven\bin\mvn.cmd" clean package -DskipTests
if exist "target\pos-1.0.0.jar" (
    start "" javaw -jar "target\pos-1.0.0.jar"
) else (
    echo Build failed. Please check logs.
    pause
)
