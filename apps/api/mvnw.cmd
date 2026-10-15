@echo off
setlocal
set "MVN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.11"
if not exist "%MVN_HOME%\bin\mvn.cmd" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $dest=Join-Path $env:USERPROFILE '.m2\wrapper\dists\apache-maven-3.9.11'; $archive=Join-Path $env:TEMP 'niteconnect-maven-3.9.11.zip'; $url='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip'; Invoke-WebRequest -Uri $url -OutFile $archive; $expected='03e2d65d4483a3396980629f260e25cac0d8b6f7f2791e4dc20bc83f9514db8d0f05b0479e699a5f34679250c49c8e52e961262ded468a20de0be254d8207076'; $actual=(Get-FileHash -Path $archive -Algorithm SHA512).Hash.ToLower(); if ($actual -ne $expected) { throw 'Checksum Maven SHA-512 invalido' }; $tempDir=Join-Path $env:TEMP ('niteconnect-maven-extract-' + [guid]::NewGuid()); New-Item -Path $tempDir -ItemType Directory -Force | Out-Null; Expand-Archive -Path $archive -DestinationPath $tempDir; New-Item -Path (Split-Path $dest -Parent) -ItemType Directory -Force | Out-Null; Move-Item -Path (Join-Path $tempDir 'apache-maven-3.9.11') -Destination $dest -ErrorAction Stop; Remove-Item -Path $archive -Force; Remove-Item -Path $tempDir -Force -Recurse"
  if errorlevel 1 exit /b 1
)
call "%MVN_HOME%\bin\mvn.cmd" %*
exit /b %errorlevel%
