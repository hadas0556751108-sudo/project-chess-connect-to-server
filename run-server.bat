@echo off
if not exist target\classes\server\ServerMain.class call build.bat
java -cp "target/classes;lib\*" server.ServerMain %*
