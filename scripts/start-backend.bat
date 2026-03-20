@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;C:\Program Files\apache-maven-3.9.11\bin;%PATH%"
cd /d "D:\学生管理系统\backend"
echo Starting backend...
echo JAVA_HOME=%JAVA_HOME%
java -version
mvn.cmd spring-boot:run -DskipTests
