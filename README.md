# Leek Wars Generator

Leek Wars fight generator using [leekscript](https://github.com/leek-wars/leekscript) language.

Forked from [leek-wars-generator](https://github.com/leek-wars/leek-wars-generator)

Used for the fight generation for [leek-wars-tools](https://github.com/Bux42/Leekwars-Tools)

This project hosts multiple different features

## Setup

Make sure you have Java 25 and gradle 9.3.1 installed

### Step 1: Init the leekscript submodule

```
git submodule sync
git submodule update --init --recursive
```

Make sure you are in the branch "leek-wars-tools"

## Step 2: Build the project

On Windows:

```
.\gradlew.bat build
```

On linux:

```
.\gradlew build
```

## Step 3: Fetch Leekwars json assets

The fight generator needs "external" json assets (weapons, chips, components etc)
To download / update those assets, you must use the --download_assets flag

```
java -jar generator_socket.jar --download_assets
```

## Make sure the generator works

There should not be any errors running the following commands

```
java -jar generator.jar --analyze test/ai/basic.leek
```

Check the following command for "Invalid AI" errors!

```
java -jar generator.jar test/scenario/scenario1.json
```

## Start the socket server

```
java -jar generator_socket.jar
```

If the server stops immediately, consider using the logs using

```
java -jar generator_socket.jar --verbose
```

## Credits

Developed by Dawyde & Pilow © 2012-2021
