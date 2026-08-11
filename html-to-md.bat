@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM Web HTML to Markdown knowledge base converter
REM Usage: html-to-md.bat <bookmarks_file | input_dir> [output_dir] [--cookie "xxx" | --cookie-file "file"]
REM   bookmarks_file: Chrome/Edge Bookmarks JSON or exported bookmarks.html (auto download)
REM   input_dir:      directory of saved HTML pages
REM Default output: src\main\resources\document

set JAVA_HOME=E:\IDE_Extesion_plugin_so_on\JAVA_JDK17

set INPUT_DIR=
set OUTPUT_DIR=
set EXTRA_ARGS=

:parse_args
if "%~1"=="" goto args_done
if "%~1"=="--cookie" (
    set EXTRA_ARGS=!EXTRA_ARGS! --cookie '%~2'
    shift
    shift
    goto parse_args
)
if "%~1"=="--cookie-file" (
    set EXTRA_ARGS=!EXTRA_ARGS! --cookie-file '%~2'
    shift
    shift
    goto parse_args
)
if "%INPUT_DIR%"=="" (
    set INPUT_DIR=%~1
) else (
    set OUTPUT_DIR=%~1
)
shift
goto parse_args
:args_done

if "%INPUT_DIR%"=="" (
    echo Usage: html-to-md.bat ^<bookmarks_file^|input_dir^> [output_dir] [--cookie "xxx" ^| --cookie-file "file"]
    echo Example: html-to-md.bat "C:\Users\xxx\AppData\Local\Google\Chrome\User Data\Default\Bookmarks"
    echo Example: html-to-md.bat web-notes --cookie-file "tmp\cookie.txt"
    pause
    exit /b 1
)

if "%OUTPUT_DIR%"=="" (
    set OUTPUT_DIR=src\main\resources\document
)

echo.
echo ======  Web HTML To Markdown Converter  ======
echo Input:  %INPUT_DIR%
echo Output: %OUTPUT_DIR%
if not "%EXTRA_ARGS%"=="" echo Extra:  %EXTRA_ARGS%
echo.

echo [1/2] Compiling project...
call mvnw compile -q -Dfile.encoding=UTF-8
if %errorlevel% neq 0 (
    echo Compile failed, please check code errors
    pause
    exit /b 1
)

echo [2/2] Converting HTML to Markdown...
call mvnw exec:java -q -Dfile.encoding=UTF-8 ^
    -Dexec.mainClass="com.example.aiagent.rag.HtmlToMarkdownConverter" ^
    -Dexec.args="--input '%INPUT_DIR%' --output '%OUTPUT_DIR%'%EXTRA_ARGS%"

echo.
echo ======  Done!  ======
echo Documents saved to: %OUTPUT_DIR%
echo Restart the backend server to load them into RAG.
pause
