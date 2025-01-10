#!/bin/bash
java -cp planetiler.jar MyProfile.java --output data/a.pmtiles --force
docker run --rm -it -v "$(pwd)/data":/data -p 8080:8080 maptiler/tileserver-gl -p 8080
