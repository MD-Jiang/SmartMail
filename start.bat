@echo off

set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

set JASYPT_ENCRYPTOR_PASSWORD=your-secret-key

if not exist data mkdir data
if not exist audio mkdir audio
if not exist logs mkdir logs

java -Xmx256m -jar target/smartmail-1.0-SNAPSHOT.jar
