param(
    [string]$JdkPath = $env:JAVA_HOME,
    [string]$DependencyCacheRoot = (Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1'),
    [string]$AndroidJarPath
)

# Fallback JVM verification when the Android Gradle toolchain cannot run.
# Does not build an APK or replace Gradle/instrumentation verification.
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$outputRoot = Join-Path $repositoryRoot '.gradle\foundation-check'
$classesPath = Join-Path $outputRoot 'classes'
New-Item -ItemType Directory -Path $classesPath -Force | Out-Null

function Copy-CachedTestJar([string]$RelativePath, [string]$FileName) {
    $sourceRoot = Join-Path $DependencyCacheRoot $RelativePath
    $sourceFile = Get-ChildItem -LiteralPath $sourceRoot -Filter $FileName -Recurse -File |
        Select-Object -First 1
    if (-not $sourceFile) { throw "Missing cached dependency: $FileName" }
    $destination = Join-Path $outputRoot $FileName
    Copy-Item -LiteralPath $sourceFile.FullName -Destination $destination -Force
    return $destination
}

if (-not $JdkPath -or -not (Test-Path -LiteralPath (Join-Path $JdkPath 'bin\javac.exe'))) {
    throw 'Provide -JdkPath or set JAVA_HOME to a JDK.'
}
$junitPath = Copy-CachedTestJar 'junit\junit\4.13.2' 'junit-4.13.2.jar'
$hamcrestPath = Copy-CachedTestJar 'org.hamcrest\hamcrest-core\1.3' 'hamcrest-core-1.3.jar'
$testClasspath = "$junitPath;$hamcrestPath"
$mainRoot = Join-Path $repositoryRoot 'app\src\main\java\com\example\nbatickethub'
$sourcePaths = @('model', 'geometry', 'data', 'state' | ForEach-Object { Join-Path $mainRoot $_ })
$mainSources = @(Get-ChildItem -LiteralPath $sourcePaths -Filter '*.java' -Recurse -File |
    Where-Object { $_.Name -ne 'AndroidAssetSource.java' -and $_.Name -ne 'ArenaSelectionViewModel.java' } | ForEach-Object FullName)
$testSources = @(Get-ChildItem -LiteralPath (Join-Path $repositoryRoot 'app\src\test\java') `
    -Filter '*.java' -Recurse -File | ForEach-Object FullName)

Push-Location $repositoryRoot
try {
    & (Join-Path $JdkPath 'bin\javac.exe') --release 11 -encoding UTF-8 -Xlint:all `
        -cp $testClasspath -d $classesPath @mainSources @testSources
    if ($LASTEXITCODE -ne 0) { throw "JVM compilation failed: $LASTEXITCODE" }
    if ($AndroidJarPath) {
        $sdkCopy = Join-Path $outputRoot 'android.jar'
        Copy-Item -LiteralPath $AndroidJarPath -Destination $sdkCopy -Force
        & (Join-Path $JdkPath 'bin\javac.exe') --release 11 -encoding UTF-8 -Xlint:all `
            -cp "$classesPath;$sdkCopy" -d $classesPath (Join-Path $mainRoot 'data\AndroidAssetSource.java')
        if ($LASTEXITCODE -ne 0) { throw "Android adapter compilation failed: $LASTEXITCODE" }
        Write-Output 'AndroidAssetSource compiled against the supplied SDK jar; not executed on Android.'
    }
    & (Join-Path $JdkPath 'bin\java.exe') -Xmx512m -cp "$classesPath;$testClasspath" `
        org.junit.runner.JUnitCore com.example.nbatickethub.data.ModelTest `
        com.example.nbatickethub.data.JsonParsingTest com.example.nbatickethub.data.ArenaDataTest `
        com.example.nbatickethub.data.RepositoryTest com.example.nbatickethub.data.ArenaPreviewDataTest `
        com.example.nbatickethub.geometry.SeatHitTesterTest com.example.nbatickethub.state.ArenaSelectionTest `
        com.example.nbatickethub.geometry.SectionHitTesterTest com.example.nbatickethub.state.MatchSeatFlowTest `
        com.example.nbatickethub.state.MatchListStateTest `
        com.example.nbatickethub.state.BrowseNavigationTest `
        com.example.nbatickethub.ExampleUnitTest
    if ($LASTEXITCODE -ne 0) { throw "JUnit failed: $LASTEXITCODE" }
}
finally { Pop-Location }
