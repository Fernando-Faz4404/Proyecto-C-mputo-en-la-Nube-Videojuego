@echo off
cd /d "%~dp0"
echo Compilando el cliente con Maven...
call mvn clean compile exec:java -Dexec.mainClass="client.ClientCloud"
pause
