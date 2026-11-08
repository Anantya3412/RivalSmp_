@echo off
mvn clean package
if errorlevel 1 pause
