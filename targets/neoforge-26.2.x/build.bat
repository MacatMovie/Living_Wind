@echo off
setlocal
cd /d "%~dp0"

call "%~dp0gradlew.bat" clean build --stacktrace
set "EXITCODE=%ERRORLEVEL%"

echo.
if not "%EXITCODE%"=="0" (
    echo BUILD FAILED with exit code %EXITCODE%.
    echo The window will stay open so you can copy the error output.
    pause
    exit /b %EXITCODE%
)

echo BUILD COMPLETE.
echo JAR should be in: build\libs\
pause
