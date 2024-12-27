# magnetron
Collapse carriageways by making them magnetic

When rendering roads with multiple [carriageways](https://en.wikipedia.org/wiki/Carriageway) at low zoom levels, for example at zoom 7 in MapLibre GL JS, one cannot visually separate the individual carriageways. They are too close together at that scale. But by default, [Planetiler](https://github.com/onthegomap/planetiler) and other vector tile generators will still include two lines for dual carriageways at all zoom levels. This increases tile size and leads to more work for the frontend rendering engine without having any cartographic benefit.

The name is inspired by https://github.com/migurski/Skeletron.

## Demo

https://wipfli.github.io/magnetron/

## Usage

Put some linestrings in a geojson file called `data/input.geojson`.

Download the planetiler jar with:

```
wget https://github.com/onthegomap/planetiler/releases/latest/download/planetiler.jar
```

Then execute:

```
./run.sh
```

and open your browser console at http://localhost:3000/

## Requirements

Java 21, tippecanoe, python3, npx

## License

Apache 2.0 for the code. Demo uses OpenStreetMap data ODbL.
