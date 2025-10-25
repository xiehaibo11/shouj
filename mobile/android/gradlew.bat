@rem Gradle startup script for Windows

@if "%DEBUG%"=="" @echo off
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
"%DIRNAME%\gradle\wrapper\gradle-wrapper.jar" %*

