#!/bin/bash
# Knowledge base document preprocessor
# Usage: ./preprocess-docs.sh <input_dir> [output_dir]
# Default output: src/main/resources/document/yuque-sync

set -e

# 切换到项目根目录（脚本位于 script/ 子目录）
cd "$(dirname "$0")/.."

JAVA_HOME="E:/IDE_Extesion_plugin_so_on/JAVA_JDK17"
export JAVA_HOME
export MAVEN_OPTS="-Dfile.encoding=UTF-8"

INPUT_DIR="$1"
OUTPUT_DIR="${2:-src/main/resources/document/yuque-sync}"

if [ -z "$INPUT_DIR" ]; then
    echo "Usage: $0 <input_dir> [output_dir]"
    echo "Example: $0 /c/Users/yourname/Desktop/my-notes"
    exit 1
fi

if [ ! -d "$INPUT_DIR" ]; then
    echo "Error: input directory not found: $INPUT_DIR"
    exit 1
fi

echo ""
echo "======  Knowledge Base Document Preprocessor  ======"
echo "Input:  $INPUT_DIR"
echo "Output: $OUTPUT_DIR"
echo ""

echo "[1/2] Compiling project..."
./mvnw compile -q

echo "[2/2] Processing documents..."
./mvnw exec:java -q \
    -Dexec.mainClass="com.example.aiagent.rag.DocumentPreprocessor" \
    -Dexec.args="--input $INPUT_DIR --output $OUTPUT_DIR"

echo ""
echo "======  Done!  ======"
echo "Documents saved to: $OUTPUT_DIR"
echo "Restart the backend server to load them into RAG."
