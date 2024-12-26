import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Envelope;

import com.onthegomap.planetiler.geo.GeoUtils;
import com.onthegomap.planetiler.util.LoopLineMerger;

public class Magnetron {

    static LineString getLinestring(double startLat, double startLon, double endLat, double endLon) {
        var coordinates = new Coordinate[2];
        coordinates[0] = new CoordinateXY(startLon, startLat);
        coordinates[1] = new CoordinateXY(endLon, endLat);
        return GeoUtils.JTS_FACTORY.createLineString(coordinates);
    }

    static List<LineString> getInput(String filePath) {
        List<LineString> result = new ArrayList<>();
        String line;
        String delimiter = ",";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                String[] values = line.split(delimiter);

                double startLon = Double.parseDouble(values[0]);
                double startLat = Double.parseDouble(values[1]);
                double endLon = Double.parseDouble(values[2]);
                double endLat = Double.parseDouble(values[3]);

                var linestring = getLinestring(startLat, startLon, endLat, endLon);
                result.add(linestring);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Point findNearestPoint(
        STRtree strTree,
        Point queryPoint,
        double searchRadius,
        List<Point> excludedPoints
    ) {
        Point nearestPoint = null;
        double minDistance = Double.MAX_VALUE;
        Envelope searchEnvelope = new Envelope(
            queryPoint.getX() - searchRadius,
            queryPoint.getX() + searchRadius,
            queryPoint.getY() - searchRadius,
            queryPoint.getY() + searchRadius
        );
        List<?> candidates = strTree.query(searchEnvelope);
        for (Object candidate : candidates) {
            Point point = (Point) candidate;
            if (excludedPoints.contains(point)) {
                continue;
            }
            double distance = queryPoint.distance(point);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPoint = point;
            }
        }
        return nearestPoint;
    }

    public static STRtree getTree(List<LineString> lines) {
        STRtree strTree = new STRtree();
        for (var line : lines) {
            for (var coordinate : line.getCoordinates()) {
                Point point = GeoUtils.JTS_FACTORY.createPoint(coordinate);
                strTree.insert(point.getEnvelopeInternal(), point);
            }
        }
        return strTree;
    }

    public static Point getMidpoint(Point p1, Point p2) {
        Coordinate c1 = p1.getCoordinate();
        Coordinate c2 = p2.getCoordinate();

        double midX = (c1.getX() + c2.getX()) / 2;
        double midY = (c1.getY() + c2.getY()) / 2;

        return GeoUtils.JTS_FACTORY.createPoint(new Coordinate(midX, midY));
    }

    public static List<LineString> magnetize(List<LineString> lines, double radius) {
        List<LineString> result = new ArrayList<>();

        STRtree strTree = getTree(lines);

        for (var line : lines) {
            var coordinates = line.getCoordinates();
            List<Point> excludedPoints = new ArrayList<>();
            for (var coordinate : coordinates) {
                Point point = GeoUtils.JTS_FACTORY.createPoint(coordinate);
                excludedPoints.add(point);
            }
            List<Point> magnetizedPoints = new ArrayList<>();
            for (var coordinate : coordinates) {
                Point queryPoint = GeoUtils.JTS_FACTORY.createPoint(coordinate);
                Point closestPoint = findNearestPoint(strTree, queryPoint, radius, excludedPoints);
                if (closestPoint == null) {
                    magnetizedPoints.add(queryPoint);
                }
                else {
                    magnetizedPoints.add(getMidpoint(queryPoint, closestPoint));
                }
            }
            result.add(GeoUtils.JTS_FACTORY.createLineString(magnetizedPoints.stream().map(Point::getCoordinate).toArray(Coordinate[]::new)));
        }

        return result;
    }

    public static List<LineString> process(List<LineString> lines, double densifyDistance) {
        List<LineString> densified = new ArrayList<>();
        for (var line : lines) {
            densified.add(LineStringDensifier.densify(line, densifyDistance));
        }

        var merger = new LoopLineMerger();
        merger.setPrecisionModel(new PrecisionModel());
        for (var line : densified) {
            merger.add(line);
        }
        var merged = merger.getMergedLineStrings();

        double radius = 2 * densifyDistance;
        var magnetized = magnetize(merged, radius);

        var merger2 = new LoopLineMerger();
        merger2.setPrecisionModel(new PrecisionModel());

        for (var line : magnetized) {
            merger2.add(line);
        }

        double loopMinLength = 5 * densifyDistance;
        merger2.setLoopMinLength(loopMinLength);
        merger2.setStubMinLength(loopMinLength);
        merger2.setTolerance(0.1 * densifyDistance);

        return merger2.getMergedLineStrings();
    }
    public static void main(String[] args) {

        String filePath = "data/input.csv";
        double densifyDistance = 0.001;

        List<LineString> lines = getInput(filePath);

        int iterations = 2;
        for (int i = 0; i < iterations; ++i) {
            lines = process(lines, densifyDistance);
        }
        
        for (var line : lines) {
            System.out.println(line);
        }
    }
}
