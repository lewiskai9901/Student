@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
cd /d D:\学生管理系统\backend
call mvn compile -q
echo EXIT_CODE=%ERRORLEVEL%
