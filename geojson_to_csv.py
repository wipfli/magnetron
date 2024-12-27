import json
import math

def read(filename):
    with open(filename) as f:
        return json.load(f)

def project_to_web_mercator(lonLat):
    lon = lonLat[0]
    lat = lonLat[1]
    x = (lon + 180) / 360

    max_lat = 85.05112878
    lat = max(min(lat, max_lat), -max_lat)

    lat_rad = math.radians(lat)
    y_mercator = math.log(math.tan(math.pi / 4 + lat_rad / 2))

    y = (1 - y_mercator / math.pi) / 2

    return [x, y]

def coordinates_to_lines(coordinates):
    lines = []
    for i in range(len(coordinates) - 1):
        start_end = project_to_web_mercator(coordinates[i])
        start_end += project_to_web_mercator(coordinates[i + 1])
        lines.append(','.join([str(n) for n in start_end]))
    return lines

def process(data):
    lines = []
    features = []
    if data["type"] == "FeatureCollection":
        features = data["features"]
    if data["type"] == "Feature":
        features = [data]

    for feature in features:
        if feature["geometry"]["type"] == "LineString":
            coordinates = feature["geometry"]["coordinates"]
            lines += coordinates_to_lines(coordinates)
        if feature["geometry"]["type"] == "MultiLineString":
            for coordinates in feature["geometry"]["coordinates"]:
                lines += coordinates_to_lines(coordinates)
    return lines

def write(lines, filename):
    with open(filename, 'w') as f:
        for line in lines:
            f.write(line + '\n')

data = read('data/input.geojson')
lines = process(data)
write(lines, 'data/input.csv')


