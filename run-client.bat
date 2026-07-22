@echo off
if not exist target\classes\client\ClientMain.class call build.bat
java -cp "target/classes;lib\*" client.ClientMain %*
