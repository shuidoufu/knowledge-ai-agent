#!/bin/bash
# Web HTML to Markdown knowledge base converter
# Usage: ./html-to-md.sh <bookmarks_file | input_dir> [output_dir] [--cookie "xxx" | --cookie-file "file"]
#   bookmarks_file: Chrome/Edge Bookmarks JSON or exported bookmarks.html (auto download)
#   input_dir:      directory of saved HTML pages
# Default output: src/main/resources/document

set -e

JAVA_HOME="E:/IDE_Extesion_plugin_so_on/JAVA_JDK17"
export JAVA_HOME
export MAVEN_OPTS="-Dfile.encoding=UTF-8"

INPUT_DIR=""
OUTPUT_DIR=""
EXTRA_ARGS=""

while [ $# -gt 0 ]; do
    case "$1" in
        --cookie)
            EXTRA_ARGS="$EXTRA_ARGS --cookie '$2'"
            shift 2
            ;;
        --cookie-file)
            EXTRA_ARGS="$EXTRA_ARGS --cookie-file '$2'"
            shift 2
            ;;
        *)
            if [ -z "$INPUT_DIR" ]; then
                INPUT_DIR="$1"
            elif [ -z "$OUTPUT_DIR" ]; then
                OUTPUT_DIR="$1"
            fi
            shift
            ;;
    esac
done

if [ -z "$INPUT_DIR" ]; then
    echo "Usage: $0 <bookmarks_file|input_dir> [output_dir] [--cookie \"xxx\" | --cookie-file \"file\"]"
    echo "Example: $0 \"/c/Users/xxx/AppData/Local/Google/Chrome/User Data/Default/Bookmarks\""
    echo "Example: $0 web-notes --cookie-file tmp/cookie.txt"
    exit 1
fi

if [ ! -d "$INPUT_DIR" ] && [ ! -f "$INPUT_DIR" ]; then
    echo "Error: input file or directory not found: $INPUT_DIR"
    exit 1
fi

if [ -z "$OUTPUT_DIR" ]; then
    OUTPUT_DIR="src/main/resources/document"
fi

echo ""
echo "======  Web HTML To Markdown Converter  ======"
echo "Input:  $INPUT_DIR"
echo "Output: $OUTPUT_DIR"
if [ -n "$EXTRA_ARGS" ]; then
    echo "Extra:  $EXTRA_ARGS"
fi
echo ""

echo "[1/2] Compiling project..."
./mvnw compile -q

echo "[2/2] Converting HTML to Markdown..."
./mvnw exec:java -q \
    -Dexec.mainClass="com.example.aiagent.rag.HtmlToMarkdownConverter" \
    -Dexec.args="--input '$INPUT_DIR' --output '$OUTPUT_DIR'$EXTRA_ARGS"

echo ""
echo "======  Done!  ======"
echo "Documents saved to: $OUTPUT_DIR"
echo "Restart the backend server to load them into RAG."
