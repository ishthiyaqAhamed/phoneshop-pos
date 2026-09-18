# Creates a Phone Shop POS Shortcut on the Windows Desktop
$desktopPath = [Environment]::GetFolderPath("Desktop")
$shortcutPath = Join-Path $desktopPath "Phone Shop POS.lnk"

$posDir = (Get-Item .).FullName
$exePath = Join-Path $posDir "dist\PhoneShopPOS\PhoneShopPOS.exe"
$batPath = Join-Path $posDir "Launch-POS.bat"

$wscript = New-Object -ComObject WScript.Shell
$shortcut = $wscript.CreateShortcut($shortcutPath)

if (Test-Path $exePath) {
    $shortcut.TargetPath = $exePath
    $shortcut.WorkingDirectory = (Join-Path $posDir "dist\PhoneShopPOS")
    $shortcut.IconLocation = "$exePath,0"
    $shortcut.Description = "Phone Shop POS & Inventory Management System"
    $shortcut.Save()
    Write-Host "Desktop Shortcut created pointing to: $exePath" -ForegroundColor Green
} else {
    $shortcut.TargetPath = $batPath
    $shortcut.WorkingDirectory = $posDir
    $shortcut.Description = "Phone Shop POS & Inventory Management System"
    $shortcut.Save()
    Write-Host "Desktop Shortcut created pointing to: $batPath" -ForegroundColor Green
}
