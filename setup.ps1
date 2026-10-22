# ============================================================
# setup.ps1 - 一键配置本项目本地构建环境（Windows）
# 功能：
#   1. 设置 JDK 17 环境变量
#   2. 解压 3rd\gradle-8.9-bin.zip 到 3rd\gradle-8.9
#   3. 校验 gradle 可运行
#   4. (可选) 生成 gradlew 包装器
# 用法：
#   powershell -ExecutionPolicy Bypass -File setup.ps1
#   powershell -ExecutionPolicy Bypass -File setup.ps1 -GenerateWrapper
# ============================================================
param(
    [switch]$GenerateWrapper
)

$ErrorActionPreference = "Stop"

# 脚本所在目录 = 项目根目录
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

# ---- 1. JDK 17 ----
$JavaHome = "C:\Program Files\Java\jdk-17.0.21.0.2"
if (-not (Test-Path (Join-Path $JavaHome "bin\java.exe"))) {
    throw "未找到 JDK: $JavaHome"
}
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
Write-Host "[1/3] JAVA_HOME = $JavaHome"

# ---- 2. 解压 Gradle ----
$Zip = Join-Path $Root "3rd\gradle-8.9-bin.zip"
$GradleDir = Join-Path $Root "3rd\gradle-8.9"
$GradleBat = Join-Path $GradleDir "bin\gradle.bat"

if (-not (Test-Path $Zip)) {
    throw "未找到 Gradle 安装包: $Zip"
}
if (-not (Test-Path $GradleBat)) {
    Write-Host "[2/3] 正在解压 Gradle -> $GradleDir ..."
    Expand-Archive -Path $Zip -DestinationPath (Join-Path $Root "3rd") -Force
}
else {
    Write-Host "[2/3] Gradle 已存在，跳过解压: $GradleBat"
}

# ---- 3. 校验 ----
Write-Host "[3/3] 校验 Gradle + JDK ..."
& $GradleBat --version

# ---- 4. (可选) 生成 wrapper ----
if ($GenerateWrapper) {
    Write-Host "正在生成 gradlew 包装器 ..."
    Push-Location $Root
    try {
        & $GradleBat wrapper --gradle-version 8.9
    }
    finally {
        Pop-Location
    }
}

Write-Host "完成。可用 $GradleBat 或 .\gradlew.bat 进行构建。"
