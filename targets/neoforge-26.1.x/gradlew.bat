@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem Living Wind project-local Gradle launcher.
rem Usage:
rem   .\gradlew build
rem   .\gradlew clean build
rem   .\gradlew build --stacktrace
rem
rem This project targets Java 21 + Gradle 8.14.4.

set "REQUIRED_GRADLE=8.14.4"
set "REQUIRED_JAVA=21"

call :selectJava
if errorlevel 1 exit /b 1

set "PATH=%JAVA_HOME%\bin;%PATH%"

call :selectGradle
if errorlevel 1 exit /b 1

echo Using JAVA_HOME=%JAVA_HOME%
echo Using Gradle=%GRADLE_EXE%
echo.

call "%GRADLE_EXE%" %*
exit /b %ERRORLEVEL%

:selectJava
rem Prefer the Java 21 location used by the project owner.
if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    exit /b 0
)

rem Otherwise keep an already-valid JAVA_HOME.
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    exit /b 0
)

rem Common alternative Java 21 installations.
for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_HOME=%%~fD"
        exit /b 0
    )
)
for /d %%D in ("C:\Program Files\Microsoft\jdk-21*") do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_HOME=%%~fD"
        exit /b 0
    )
)

if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    exit /b 0
)

echo ERROR: Java 21 could not be found automatically.
echo Expected Java at C:\Program Files\Java\jdk-21 or in JAVA_HOME.
echo Install Java 21 or set JAVA_HOME, then run .\gradlew again.
exit /b 1

:selectGradle
rem Prefer GRADLE_HOME when it points at the required version.
if defined GRADLE_HOME if exist "%GRADLE_HOME%\bin\gradle.bat" (
    set "GRADLE_EXE=%GRADLE_HOME%\bin\gradle.bat"
    exit /b 0
)

rem Project owner's installed Gradle location.
if exist "C:\Gradle\gradle-%REQUIRED_GRADLE%\bin\gradle.bat" (
    set "GRADLE_EXE=C:\Gradle\gradle-%REQUIRED_GRADLE%\bin\gradle.bat"
    exit /b 0
)

rem Last fallback: Gradle available on PATH.
where gradle.bat >nul 2>nul
if not errorlevel 1 (
    for /f "delims=" %%G in ('where gradle.bat') do (
        set "GRADLE_EXE=%%G"
        goto :gradleFound
    )
)

echo ERROR: Gradle %REQUIRED_GRADLE% could not be found.
echo Expected it at C:\Gradle\gradle-%REQUIRED_GRADLE%\bin\gradle.bat,
echo via GRADLE_HOME, or on PATH.
exit /b 1

:gradleFound
exit /b 0
