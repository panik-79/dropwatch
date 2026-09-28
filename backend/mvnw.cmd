@echo off
@REM DropWatch Maven Wrapper script for Windows

setlocal

if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  if exist "C:\Program Files\Java\jdk-25\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    set "JAVA_EXE=C:\Program Files\Java\jdk-25\bin\java.exe"
  ) else (
    set "JAVA_EXE=java"
  )
)

set "MVN_CMD=C:\Users\pujan\.gemini\antigravity-ide\scratch\tools\apache-maven-3.9.9\bin\mvn.cmd"

if exist "%MVN_CMD%" (
  "%MVN_CMD%" %*
) else (
  mvn %*
)
