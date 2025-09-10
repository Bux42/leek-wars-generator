# Leek Wars Generator

A fork of the official [leek-wars-generator](https://github.com/leek-wars/leek-wars-generator) project.

The goal is to benchmark different AIs, log the data, and find the "best" AIs

You must delete the leekscript folder and replace it with a clone from the original [leekscript](https://github.com/leek-wars/leekscript) project.

And then compile leekscript:

```
cd leekscript
gradle jar
```

Then you can compile the generator project
Make sure to uncomment the line

```
Data.checkData("https://leekwars.com/api/");
```

To load API Data for weapons / chips etc

Then recompile at root folder

```
gradle jar
```

Then run any scenario to fetch the json files

```
java -jar generator.jar test/scenario/scenario1.json --verbose
```

Then comment it again xddd

Then recompile again

```
gradle jar
```

## Added features

You can now run the same scenario multiple times to get the "average" winrate

```
java -jar generator.jar test/scenario/scenario1.json --run_count=100
```

## Build

gradle jar

```

## AI analysis task

```

java -jar generator.jar --analyze test/ai/basic.leek

```

![Fight generation task](https://github.com/leek-wars/leek-wars-generator-v1/blob/master/doc/compilation_task.svg)

## Fight generation task

```

java -jar generator.jar test/scenario/scenario1.json

```

![Fight generation task](https://github.com/leek-wars/leek-wars-generator-v1/blob/master/doc/fight_task.svg)

## Credits

Developed by Dawyde & Pilow © 2012-2021

```

```

```
