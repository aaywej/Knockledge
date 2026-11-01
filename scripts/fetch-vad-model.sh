#!/usr/bin/env bash
# Downloads the Silero VAD ONNX model into androidApp/src/main/assets/ for bundling into the APK.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
URL="https://github.com/snakers4/silero-vad/raw/v5.1.2/src/silero_vad/data/silero_vad.onnx"
OUT_DIR="${SCRIPT_DIR}/../androidApp/src/main/assets"
OUT_FILE="${OUT_DIR}/silero_vad.onnx"

mkdir -p "${OUT_DIR}"

if [ -s "${OUT_FILE}" ]; then
    echo "Silero VAD ONNX model already exists at ${OUT_FILE}."
    exit 0
fi

echo "Downloading Silero VAD ONNX model..."
curl -sSL "${URL}" -o "${OUT_FILE}"
echo "Downloaded Silero VAD ONNX model to ${OUT_FILE}."
