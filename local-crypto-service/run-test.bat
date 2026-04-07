@echo off
cd /d %~dp0
echo Starting Local Crypto Service...
start /B java -cp "target/classes;%USERPROFILE%\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar" com.qar.crypto.LocalCryptoService > service.log 2>&1
timeout /t 3 /nobreak > nul
echo Service started. Check service.log for output.
