#!/bin/bash
java -cp planetiler.jar MyProfile.java --output data/boston.pmtiles --force
serve --debug . -p 3000 --cors
