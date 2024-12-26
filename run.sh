#!/bin/bash
python3 geojson_to_csv.py
java -cp planetiler.jar Magnetron.java > data/magnetized.wkt
python3 wkt_to_geojson.py
tippecanoe -o data/magnetized.pmtiles data/magnetized.geojson --layer magnetized --force --maximum-zoom 15 --minimum-zoom 6 --drop-densest-as-needed
docker run --rm -it -v "$(pwd)/data":/data -p 8080:8080 maptiler/tileserver-gl -p 8080
