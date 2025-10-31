param()
$mvn = Get-Command mvn -ErrorAction Stop
mvn -q -pl app-ui -am -DskipTests package
$jar = Join-Path $PSScriptRoot "..\app-ui\target\bidinote.jar"
java -cp $jar com.bidinote.ui.tools.SeedGenerator
