@echo off
if not exist target\classes mkdir target\classes
if not exist lib (
    echo lib folder not found. Please place the required jars there.
    pause
    exit /b 1
)

if "%~1"=="" (
    set MAIN_CLASS=Main
) else (
    set MAIN_CLASS=%~1
)

dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\*" -d target/classes @sources.txt
if %errorlevel% neq 0 (
    echo Compilation failed
    del sources.txt 2>nul
    pause
    exit /b %errorlevel%
)
del sources.txt 2>nul

java -cp "target/classes;lib\*" %MAIN_CLASS%
