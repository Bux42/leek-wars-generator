# Leek Wars Generator Laboratory

Leek Wars fight generator using [leekscript](https://github.com/leek-wars/leekscript) language.

Forked from [leek-wars-generator](https://github.com/leek-wars/leek-wars-generator)

This project hosts multiple different features

## Setup

Make sure you have Java 24 installed

### Step 1: Init the leekscript submodule

```
git submodule sync
git submodule update --init --recursive
```

Make sure you are in the branch "pool_generator_backup"

## Step 2: Build the project

```
gradle jar
```

## Step 3: Fetch Leekwars json assets

The fight generator needs "external" json assets (weapons, chips, components etc)
To download / update those assets, you must use the --download_assets flag

```
java -jar generator.jar --download_assets
```

## Run example fight scenario

```
java -jar generator.jar test/scenario/scenario1.json
```

## AI analysis task

```
java -jar generator.jar --analyze test/ai/basic.leek
```

## Multiple runs

Execute the same scenario multiple times

```
java -jar generator.jar .test/scenario/scenario1.json --run_count=10
```

Simple output:

```
run_count: 10
Player 0 win: 5
Player 1 win: 5
Draw: 0
```

## Deterministic mode

Useful when executing the same scenario multiple times using --run_count
When the map, placements, damages etc are the same, it's easy to benchmark different versions of the same AI

```
java -jar generator.jar .test/scenario/scenario1.json --run_count=10 --deterministic
```

## Pool mode

Benchmark X fight AIs with X leek builds

You can make & export leek builds using https://leek-wars-restator.vercel.app/ (the save button on the right saves the current build as json)

Calculate elo

TODO:

- In the pool loop, find scenario with the two leeks that have the lowest fight count with the closest elo?
- Instead of giving the root folder containing the AIs, add ability to select specific .leek files
- At the moment the pool feature only work for 1v1, implement 4v4, 6v6, battle royal, boss etc

Usage:

`--leek_builds_path` is the path to the directory containing the different leek builds you want to bench
`--ais_folder_path` is the path to the directory containing the different leek ais you want to bench

```
java -jar generator.jar --leek_builds_path="./test/leeks" --ais_folder_path="./test/ai/poolTest" --run_count=10
```

Output:

```
PoolManager: Completed 13200 fights.
+--------------------------+---------------------------------------------------------------+--------+
| Leek name                | AI path                                                       | Elo    |
+--------------------------+---------------------------------------------------------------+--------+
| lvl_1_300_life           | .\test\ai\poolTest\basic_leek_triple_pistol.leek              | 2109   |
| lvl_1_100_strength       | .\test\ai\poolTest\basic_leek_triple_pistol.leek              | 1596   |
| lvl_1_100_widsom         | .\test\ai\poolTest\basic_leek_triple_pistol.leek              | 1199   |
| lvl_1_300_life           | .\test\ai\poolTest\basic.leek                                 | 412    |
| lvl_1_no_stats           | .\test\ai\poolTest\basic_leek_triple_pistol.leek              | 410    |
| lvl_1_100_resistance     | .\test\ai\poolTest\basic_leek_triple_pistol.leek              | 381    |
| lvl_1_100_agility        | .\test\ai\poolTest\basic_leek_triple_pistol.leek              | 364    |
| lvl_1_100_strength       | .\test\ai\poolTest\basic.leek                                 | -343   |
| lvl_1_100_widsom         | .\test\ai\poolTest\basic.leek                                 | -744   |
| lvl_1_no_stats           | .\test\ai\poolTest\basic.leek                                 | -1385  |
| lvl_1_100_agility        | .\test\ai\poolTest\basic.leek                                 | -1399  |
| lvl_1_100_resistance     | .\test\ai\poolTest\basic.leek                                 | -1400  |
+--------------------------+---------------------------------------------------------------+--------+
```

## Http server

The http server is home to multiple features.

Mainly, it serves as the code analyzer for the leekswars vscode extension.

More information on the extension here: [here](https://github.com/Bux42/leekscript-vscode)

```
java -jar generator.jar --start_code_server
```

You can also specify which port the http server will use (default is 8080)
You will also have to configure the server url / port accordingly in the vscode extension if you wish to use another port

```
java -jar generator.jar --start_code_server --code_server_port=1234
```

## Test definitions on a specific file at specific user cursor pos & file

The definition feature serves the vscode extension
For a given file, at a given line and column, the extension will ask the server for definitions (for autocomplete, goto definition, getting info about a variable / class etc)

```
gradle jar ; java -jar generator.jar --get_definitions_file=user-code/Basic.leek --get_definitions_cursor_line=2 --get_definitions_cursor_column=1 --get_definitions
```

Additionnaly, if you can enable logs during the definition fetching process by adding the following flag

```
--debug_definitions
```

## Start definitions tester

Start all definitions tests (dev feature)

gradle jar ; java -jar generator.jar --test_definitions

## Generate json files to visualize fight in the official leek wars vue front end project

It will generate both the fight data and the logs (debug in leekscript)

gradle jar;java -jar generator.jar .\test\scenario\scenario_4v4.json --fight_json_output_directory=fight_outcome

gradle jar;java -jar generator.jar .\test\scenario\scenario_4v4.json --fight_json_output_directory=fight_outcome --nocache;Copy-Item .\fight_outcome\fight_0.json D:\Github\leek-wars\src\component\fight;Copy-Item .\fight_outcome\fight_0_logs.json D:\Github\leek-wars\src\component\fight

## Credits

Developed by Dawyde & Pilow © 2012-2021
