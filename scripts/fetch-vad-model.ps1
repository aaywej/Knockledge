# Downloads the Silero VAD ONNX model for the JVM unit test OnnxSileroVadDetectorModelTest.
# The target directory is gitignored; on device the same file comes through ModelStore.
$ErrorActionPreference = 'Stop'

$url = 'https://github.com/snakers4/silero-vad/raw/v5.1.2/src/silero_vad/data/silero_vad.onnx'
$outDir = Join-Path $PSScriptRoot '..\androidApp\src\test\resources-local'
$outFile = Join-Path $outDir 'silero_vad.onnx'

if ((Test-Path $outFile) -and (Get-Item $outFile).Length -gt 0) {
    Write-Host "Already present: $outFile"
    exit 0
}

New-Item -ItemType Directory -Force $outDir | Out-Null
Invoke-WebRequest -Uri $url -OutFile $outFile
Write-Host "Downloaded $((Get-Item $outFile).Length) bytes to $outFile"
