#!/bin/bash
# python3 geojson_to_csv.py
java -cp planetiler.jar Magnetron.java > data/magnetized.wkt
python3 wkt_to_geojson.py
tippecanoe -o data/magnetized.pmtiles data/magnetized.geojson --layer magnetized --force --maximum-zoom 15 --minimum-zoom 6 --drop-densest-as-needed
npx serve --debug . -p 3000 --cors
