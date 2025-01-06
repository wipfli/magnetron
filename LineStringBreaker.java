import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.GeometryFactory;

import com.onthegomap.planetiler.geo.GeoUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

public class LineStringBreaker {

    // Function to break a LineString into multiple parts based on excluded coordinates
    public static List<LineString> breakLineString(LineString line, Set<Coordinate> excludedCoordinates, double eraseMinLength) {
        List<LineString> result = new ArrayList<>();

        List<List<Coordinate>> includedParts = new ArrayList<>();

        List<Coordinate> currentPart = new ArrayList<>();

        var coordinates = line.getCoordinates();
        if (coordinates.length == 0) {
            return result;
        }
        boolean currentPartIsIncluded = !excludedCoordinates.contains(coordinates[0]);
        for (Coordinate coord : line.getCoordinates()) {
            if (excludedCoordinates.contains(coord)) {
                if (currentPartIsIncluded) {
                    includedParts.add(List.copyOf(currentPart));
                    currentPartIsIncluded = false;
                    currentPart.clear();
                }
            }
            else {
                if (!currentPartIsIncluded) {
                    boolean aboveMinLength = true;
                    if (currentPart.size() >= 2) {
                        LineString excludedLine = GeoUtils.JTS_FACTORY.createLineString(currentPart.toArray(Coordinate[]::new));
                        if (excludedLine.getLength() <= eraseMinLength) {
                            aboveMinLength = false;
                        }
                    }
                    else if (currentPart.size() == 1) {
                        aboveMinLength = false;
                    }
                    currentPartIsIncluded = true;
                    if (aboveMinLength) {
                        currentPart.clear();
                    }
                    else {
                        if (includedParts.size() > 0) {
                            var last = new ArrayList<>(includedParts.getLast());
                            last.addAll(currentPart);
                            currentPart = new ArrayList<>(last);
                            includedParts.remove(includedParts.size() - 1);
                        }
                    }
                }
            }
            currentPart.add(coord);
        }

        if (currentPartIsIncluded) {
            includedParts.add(currentPart);
        }
        else {
            boolean aboveMinLength = true;
            if (currentPart.size() >= 2) {
                LineString excludedLine = GeoUtils.JTS_FACTORY.createLineString(currentPart.toArray(Coordinate[]::new));
                if (excludedLine.getLength() <= eraseMinLength) {
                    aboveMinLength = false;
                }
            }
            else if (currentPart.size() == 1) {
                aboveMinLength = false;
            }

            if (!aboveMinLength) {
                if (includedParts.size() > 0) {
                    var last = new ArrayList<>(includedParts.getLast());
                    last.addAll(currentPart);
                    currentPart = new ArrayList<>(last);
                    includedParts.remove(includedParts.size() - 1);
                }
                includedParts.add(currentPart);
            }
        }

        for (var includedPart : includedParts) {
            if (includedPart.size() < 2) {
                continue;
            }
            LineString includedLine = GeoUtils.JTS_FACTORY.createLineString(includedPart.toArray(Coordinate[]::new));
            result.add(includedLine);
        }

        return result;
    }

    public static void main(String[] args) {
        // Example usage

        // Create a LineString and a set of excluded coordinates
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(2, 0),
                new Coordinate(2.5, 0),
                new Coordinate(3, 0),
                new Coordinate(4, 0)
        };
        LineString line = geometryFactory.createLineString(coords);

        Set<Coordinate> excludedCoordinates = new HashSet<>();
        excludedCoordinates.add(new Coordinate(3.0, 0)); 
        excludedCoordinates.add(new Coordinate(4.0, 0));

        // Break the line
        List<LineString> brokenParts = breakLineString(line, excludedCoordinates, 2.0);

        // Output the broken parts
        for (LineString part : brokenParts) {
            System.out.println(part);
        }
    }
}
