@echo off
echo ========================================
echo   Plateforme Pedagogique avec IA
echo   Lancement de l'application...
echo ========================================
echo.

REM Vérifier que Maven est installé
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] Maven n'est pas installe ou pas dans le PATH
    echo Telechargez Maven depuis: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Vérifier que Java est installé
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] Java n'est pas installe ou pas dans le PATH
    echo Telechargez Java 17+ depuis: https://adoptium.net/
    pause
    exit /b 1
)

echo [OK] Java trouve: 
java -version 2>&1 | findstr /i "version"
echo.

echo [OK] Maven trouve:
mvn -version 2>&1 | findstr /i "Apache Maven"
echo.

echo ========================================
echo   Compilation du projet...
echo ========================================
echo.

call mvn clean install -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] La compilation a echoue
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Lancement de l'application...
echo ========================================
echo.
echo L'application sera accessible sur:
echo   http://localhost:8080
echo.
echo Comptes de demonstration:
echo   Admin:    admin / admin123
echo   Etudiant: student / student123
echo.
echo Appuyez sur Ctrl+C pour arreter l'application
echo ========================================
echo.

call mvn spring-boot:run
