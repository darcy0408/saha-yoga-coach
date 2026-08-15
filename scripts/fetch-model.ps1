# Downloads the pose model. Run once before first use.
#
# MoveNet SinglePose Lightning: 17 body keypoints, ~9 MB, fast enough for real
# time on a CPU. Weights are not committed to git - they are large, and they are
# not ours. No account or API key required.
#
# The checksum below is the artifact this project validated against; a mismatch
# means the upstream file changed and the tensor layout must be re-checked
# before the coach is allowed to use it.

$ErrorActionPreference = 'Stop'

$url    = 'https://huggingface.co/Xenova/movenet-singlepose-lightning/resolve/main/onnx/model.onnx'
$sha256 = '1AD4F8D6C2F776A9967DB3993C9CA740BC350104F9D37C151DC183FC29A464AD'
$dir    = Join-Path $PSScriptRoot '..\models'
$out    = Join-Path $dir 'movenet-singlepose-lightning.onnx'

if (-not (Test-Path $dir)) { New-Item -ItemType Directory $dir | Out-Null }

if (Test-Path $out) {
    Write-Output "Model already present: $out"
} else {
    Write-Output "Downloading MoveNet SinglePose Lightning (~9 MB)..."
    Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing
}

$actual = (Get-FileHash $out -Algorithm SHA256).Hash
if ($actual -ne $sha256) {
    Write-Error "Checksum mismatch.`n  expected $sha256`n  actual   $actual`nDelete the file and try again; do not use an unverified model."
    exit 1
}

$sizeMb = (Get-Item $out).Length / 1MB
Write-Output ("Verified {0} ({1:N1} MB, SHA-256 matches)" -f $out, $sizeMb)
Write-Output ""
Write-Output "Now run:  .\gradlew.bat run   then enable the camera during setup."
