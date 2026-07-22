@echo off
if not exist target\classes mkdir target\classes
if not exist lib (
    echo lib folder not found. Please place the required jars there.
    exit /b 1
)

javac -encoding UTF-8 -cp "lib\*" -sourcepath src -d target/classes src\Main.java src\client\ClientMain.java src\app\SingleProcessLauncher.java src\server\ServerMain.java
if %errorlevel% neq 0 (
    echo Compilation failed
    exit /b %errorlevel%
)

echo Build completed.
