param([string]$JdkPath = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
if (-not $JdkPath) { $JdkPath = 'C:/Program Files/Android/Android Studio/jbr' }
$classes = Join-Path $repositoryRoot '.gradle/preview-export/classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$main = Join-Path $repositoryRoot 'app/src/main/java/com/example/nbatickethub'
$sources = @('model', 'geometry', 'data' | ForEach-Object { Join-Path $main $_ })
$javaFiles = @(Get-ChildItem -LiteralPath $sources -Filter '*.java' -Recurse -File |
    Where-Object Name -NE 'AndroidAssetSource.java' | ForEach-Object FullName)
# The relative match schedule uses the same pure-Java device-time formatter as Android.
$javaFiles += Join-Path $main 'state/MatchListState.java'
$exporter = Join-Path $PSScriptRoot 'java/ArenaPreviewExport.java'
& "$JdkPath/bin/javac.exe" --release 11 -encoding UTF-8 -d $classes @javaFiles $exporter
if ($LASTEXITCODE -ne 0) { throw 'Preview exporter compilation failed.' }
& "$JdkPath/bin/java.exe" -Xmx512m -cp $classes ArenaPreviewExport `
    (Join-Path $repositoryRoot 'app/src/main/assets') `
    (Join-Path $repositoryRoot 'preview/arena-preview.template.html') `
    (Join-Path $repositoryRoot 'preview/arena-preview.html')
if ($LASTEXITCODE -ne 0) { throw 'Preview export failed.' }
Write-Output 'Open preview/arena-preview.html directly; no server, npm or emulator required.'
