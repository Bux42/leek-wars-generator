# Profiler setup on Windows

Setup target: `profiler` branch of `leek-wars-generator`, `profiler` branch of `leekscript`, and `FlameGraph` submodule.

## Requirements

- Windows 10 or newer
- Git for Windows
- JDK 25
- Perl, such as Strawberry Perl
- Internet access to GitHub and Maven Central

Set `JAVA_HOME` to JDK 25 installation directory. Add `%JAVA_HOME%\bin` to `Path`.

Check setup in PowerShell:

```powershell
java -version
git --version
perl --version
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

`git submodule update --remote` uses `branch = profiler` for `leekscript`. `FlameGraph` follows its remote default branch.

Verify submodule configuration:

```powershell
git config -f .gitmodules --get submodule.leekscript.url
git config -f .gitmodules --get submodule.leekscript.branch
git config -f .gitmodules --get submodule.FlameGraph.url
git -C leekscript remote -v
```

Expected values:

```text
https://github.com/Bux42/leekscript-local.git
profiler
https://github.com/brendangregg/FlameGraph.git
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

## Profiler output

Every fight writes profiler data under:

```text
profiler-output/<timestamp>/<turn>/<entity name>_<entity id>.folded
```

Each file uses folded-stack format. One line contains a semicolon-separated stack and consumed OP count:

```text
runIA (basic.leek:1);ClassA.ClassB.methodA (basic.leek:20) 42
```

Files can be rendered with `flamegraph.pl`:
(you will need perl / Strawberry Perl installed to run that script)

```powershell
perl .\FlameGraph\flamegraph.pl --width 1920 --countname OPs --colors js `
    .\profiler-output\<timestamp>\<turn>\<entity name>_<entity id>.folded `
    > .\profile.svg
```

Only alive, valid entities that execute a turn get files. Bulbs use their own entity file. `staticInit` appears when it consumes OP.

## Sync official changes

Add official remotes once:

```powershell
git remote add upstream https://github.com/leek-wars/leek-wars-generator.git
git -C leekscript remote add upstream https://github.com/leek-wars/leekscript.git
```

Update generator `master`, then rebase profiler branch:

```powershell
git fetch upstream master
git switch master
git reset --hard upstream/master
git switch profiler
git rebase master
```

Update `leekscript` official base, then rebase profiler instrumentation:

```powershell
git -C leekscript fetch upstream master
git -C leekscript switch master
git -C leekscript reset --hard upstream/master
git -C leekscript switch profiler
git -C leekscript rebase master
```

Record updated submodule commit in generator:

```powershell
git add leekscript
git commit -m "Update leekscript submodule"
.\gradlew.bat jar
```

Resolve conflicts by keeping official changes and reapplying profiler hooks. `git submodule update --remote` follows fork `profiler` branch; use it only after fork branch receives synced profiler commits.
