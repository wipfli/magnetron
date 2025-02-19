#!/bin/bash
python3 geojson_to_csv.py
java -cp planetiler.jar Run.java > data/magnetized.wkt
python3 wkt_to_geojson.py
tippecanoe -o data/magnetized-3-erased.pmtiles data/magnetized.geojson --layer magnetized --force --maximum-zoom 14 --minimum-zoom 0 --drop-densest-as-needed
