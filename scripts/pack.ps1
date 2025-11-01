param()
$mvn = Get-Command mvn -ErrorAction Stop
mvn -q -U -DskipTests package
$dist = Join-Path $PSScriptRoot "..\dist"
if (-not (Test-Path $dist)) { New-Item -ItemType Directory -Path $dist | Out-Null }
Copy-Item ..\app-ui\target\bidinote.jar $dist\bidinote-with-deps.jar -Force
Write-Host "已生成 dist\\bidinote-with-deps.jar"
