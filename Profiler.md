# Profiler setup on Windows

Setup target: `profiler` branch of `leek-wars-generator` and `profiler` branch of the `leekscript` submodule.

## Requirements

- Windows 10 or newer
- Git for Windows
- JDK 25
- Internet access to GitHub and Maven Central

Set `JAVA_HOME` to JDK 25 installation directory. Add `%JAVA_HOME%\bin` to `Path`.

Check setup in PowerShell:

```powershell
java -version
git --version
```

Project wrapper downloads Gradle 9.1.0. No separate Gradle installation required.

## Clone project

Run commands in PowerShell:

```powershell
git clone --branch profiler --recurse-submodules https://github.com/Bux42/leek-wars-generator.git
Set-Location leek-wars-generator
git submodule sync --recursive
git submodule update --init --remote --recursive
```

`git submodule update --remote` uses `branch = profiler` from `.gitmodules`.

Verify submodule configuration:

```powershell
git config -f .gitmodules --get submodule.leekscript.url
git config -f .gitmodules --get submodule.leekscript.branch
git -C leekscript remote -v
```

Expected values:

```text
https://github.com/Bux42/leekscript-local.git
profiler
```

## Build generator

Run from repository root:

```powershell
.\gradlew.bat jar
```

Build creates `generator.jar` in repository root.

## Execute fight

Run from repository root:

```powershell
java -jar generator.jar test/scenario/scenario1.json .
```

Generator writes fight result JSON to standard output.

## Update profiler code

```powershell
git switch profiler
git pull
git submodule sync --recursive
git submodule update --init --remote --recursive
.\gradlew.bat jar
```

