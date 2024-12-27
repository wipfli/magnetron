import json
import math

def web_mercator_to_lon_lat(xy):
    x = xy[0]
    y = xy[1]
    lon = x * 360 - 180
    lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * y)))
    lat = math.degrees(lat_rad)
    return [lon, lat]

def linestring_to_geometry(wkt_linestring):
    coords_text = wkt_linestring.replace("LINESTRING", "").replace("(", "").replace(")", "").strip()
    coordinates = [
        list(map(float, pair.split()))
        for pair in coords_text.split(",")
    ]
    geometry = {
        "type": "LineString",
        "coordinates": [web_mercator_to_lon_lat(c) for c in coordinates]
    }
    return geometry

def process(in_filename, out_filename):
    features = []
    with open(in_filename) as f:
        line = f.readline()
        while line != '':
            geometry = linestring_to_geometry(line)
            features.append({
                "type": "Feature",
                "geometry": geometry,
                "properties": {}
            })
            line = f.readline()
    
    with open(out_filename, 'w') as f:
        json.dump({
            "type": "FeatureCollection",
            "features": features
        }, f)

process('data/magnetized.wkt', 'data/magnetized.geojson')
