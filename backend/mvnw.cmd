@echo off
setlocal
cd /d "%~dp0"

where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

where docker >nul 2>nul
if %ERRORLEVEL%==0 (
  docker run --rm -v "%cd%:/app" -w /app maven:3.9-eclipse-temurin-21 mvn %*
  exit /b %ERRORLEVEL%
)

echo Maven is not installed, and Docker is not available to run Maven.
echo Install Java 21 + Maven, or install Docker Desktop, then retry.
exit /b 1
