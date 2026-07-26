@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM Knowledge base document preprocessor
REM Usage: preprocess-docs.bat <input_dir> [output_dir]
REM Default output: src\main\resources\document\yuque-sync

set JAVA_HOME=E:\IDE_Extesion_plugin_so_on\JAVA_JDK17

set INPUT_DIR=%1
set OUTPUT_DIR=%2

if "%INPUT_DIR%"=="" (
    echo Usage: preprocess-docs.bat ^<input_dir^> [output_dir]
    echo Example: preprocess-docs.bat D:\my-notes src\main\resources\document\yuque-sync
    pause
    exit /b 1
)

if "%OUTPUT_DIR%"=="" (
    set OUTPUT_DIR=src\main\resources\document\yuque-sync
)

echo.
echo ======  Knowledge Base Document Preprocessor  ======
echo Input:  %INPUT_DIR%
echo Output: %OUTPUT_DIR%
echo.

echo [1/2] Compiling project...
call mvnw compile -q -Dfile.encoding=UTF-8
if %errorlevel% neq 0 (
    echo Compile failed, please check code errors
    pause
    exit /b 1
)

echo [2/2] Processing documents...
call mvnw exec:java -q -Dfile.encoding=UTF-8 ^
    -Dexec.mainClass="com.example.aiagent.rag.DocumentPreprocessor" ^
    -Dexec.args="--input %INPUT_DIR% --output %OUTPUT_DIR%"

echo.
echo ======  Done!  ======
echo Documents saved to: %OUTPUT_DIR%
echo Restart the backend server to load them into RAG.
pause
