# Downloads the pose model. Run once before first use.
#
# MoveNet SinglePose Lightning: 17 body keypoints, ~9 MB, fast enough for real
# time on a CPU. Weights are not committed to git - they are large, and they are
# not ours. No account or API key required.
#
# -Thunder additionally fetches the MoveNet SinglePose Thunder candidate
# (256-pixel input, ~25 MB) - the fallback if the person crop alone does not
# recover overhead wrists and seated legs. Fetching it enables nothing: the
# application only ever looks for Lightning by name, and using Thunder takes
# setting the saha.model system property deliberately, once it has been
# validated on a real body.
#
# The checksums below are the artifacts this project validated against; a
# mismatch means the upstream file changed and the tensor layout must be
# re-checked before the coach is allowed to use it.

param([switch]$Thunder)

$ErrorActionPreference = 'Stop'

$dir = Join-Path $PSScriptRoot '..\models'
if (-not (Test-Path $dir)) { New-Item -ItemType Directory $dir | Out-Null }

function Fetch-Verified([string]$name, [string]$url, [string]$sha256, [string]$sizeNote) {
    $out = Join-Path $dir $name
    if (Test-Path $out) {
        Write-Output "Model already present: $out"
    } else {
        Write-Output "Downloading $name ($sizeNote)..."
        Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing
    }
    $actual = (Get-FileHash $out -Algorithm SHA256).Hash
    if ($actual -ne $sha256) {
        Write-Error "Checksum mismatch for $name.`n  expected $sha256`n  actual   $actual`nDelete the file and try again; do not use an unverified model."
        exit 1
    }
    $sizeMb = (Get-Item $out).Length / 1MB
    Write-Output ("Verified {0} ({1:N1} MB, SHA-256 matches)" -f $out, $sizeMb)
}

Fetch-Verified 'movenet-singlepose-lightning.onnx' `
    'https://huggingface.co/Xenova/movenet-singlepose-lightning/resolve/main/onnx/model.onnx' `
    '1AD4F8D6C2F776A9967DB3993C9CA740BC350104F9D37C151DC183FC29A464AD' '~9 MB'

if ($Thunder) {
    Fetch-Verified 'movenet-singlepose-thunder.onnx' `
        'https://huggingface.co/Xenova/movenet-singlepose-thunder/resolve/main/onnx/model.onnx' `
        '3DCA9F6E5F8A64DC9935A5BE06FD8BF81BF01E696C9C05C6F2A650E0A401B763' '~25 MB'
}

Write-Output ""
Write-Output "Now run:  .\gradlew.bat run   then enable the camera during setup."
