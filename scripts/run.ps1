param()
$jar = Join-Path $PSScriptRoot "..\app-ui\target\bidinote.jar"
if (-not (Test-Path $jar)) {
    Write-Host "JAR 未找到，请先运行 mvn -q -U -DskipTests package"
    exit 1
}
java -jar $jar
