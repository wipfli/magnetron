import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Envelope;

import com.onthegomap.planetiler.geo.GeoUtils;
import com.onthegomap.planetiler.util.LoopLineMerger;

public class Magnetron {
    private final List<LineString> input = new ArrayList<>();
    private double loopMinLength = 0.0;
    private double radius = 0.0;
    private double densifyDistance = 0.0;
    private int iterations = 0;
    private double tolernace = 0.0;

    public Magnetron add(LineString line) {
        input.add(line);
        return this;
    }

    public Magnetron setLoopMinLength(double loopMinLength) {
        this.loopMinLength = loopMinLength;
        return this;
    }

    public Magnetron setRadius(double radius) {
        this.radius = radius;
        return this;
    }

    public Magnetron setDensifyDistance(double densifyDistance) {
        this.densifyDistance = densifyDistance;
        return this;
    }

    public Magnetron setIterations(int iterations) {
        this.iterations = iterations;
        return this;
    }

    public Magnetron setTolerance(double tolernace) {
        this.tolernace = tolernace;
        return this;
    }

    private List<Point> findClosePoints(
        STRtree strTree,
        Point queryPoint
    ) {
        List<Point> result = new ArrayList<>();
        Envelope searchEnvelope = new Envelope(
            queryPoint.getX() - radius,
            queryPoint.getX() + radius,
            queryPoint.getY() - radius,
            queryPoint.getY() + radius
        );
        List<?> candidates = strTree.query(searchEnvelope);
        for (var c : candidates) {
            if (c instanceof Point p) {
                result.add(p);
            }
        }
        return result;
    }

    private static STRtree getTree(List<LineString> lines) {
        STRtree strTree = new STRtree();
        for (var line : lines) {
            for (var coordinate : line.getCoordinates()) {
                Point point = GeoUtils.JTS_FACTORY.createPoint(coordinate);
                strTree.insert(point.getEnvelopeInternal(), point);
            }
        }
        return strTree;
    }

    private static Point getMidpoint(List<Point> points) {
        double midX = 0.0;
        double midY = 0.0;
        for (Point point : points) {
            Coordinate coordinate = point.getCoordinate();
            midX += coordinate.getX();
            midY += coordinate.getY();
        }
        if (points.size() > 0) {
            midX /= points.size();
            midY /= points.size();
        }
        return GeoUtils.JTS_FACTORY.createPoint(new Coordinate(midX, midY));
    }

    private static Point getMidpoint(List<Point> points, Map<Point, Double> weights) {
        double weightedSumX = 0.0;
        double weightedSumY = 0.0;
        double totalWeight = 0.0;
    
        for (Point point : points) {
            double weight = weights.get(point);
            // System.out.println(weight);
            Coordinate coordinate = point.getCoordinate();
            weightedSumX += coordinate.getX() * weight;
            weightedSumY += coordinate.getY() * weight;
            totalWeight += weight;
        }
    
        if (totalWeight > 0.0) {
            weightedSumX /= totalWeight;
            weightedSumY /= totalWeight;
        }
        else {
            System.out.println("totalWeigth is zero " + weights.get(points.getLast()));
        }
    
        return GeoUtils.JTS_FACTORY.createPoint(new Coordinate(weightedSumX, weightedSumY));
    }

    private static Point getMidpoint(Point A, Point B) {
        List<Point> points = new ArrayList<>();
        points.add(A);
        points.add(B);
        return getMidpoint(points);
    }

    private static Map<Point, Double> getPointToAngleMap(List<LineString> lines) {
        Map<Point, Double> map = new HashMap<>();
        for (var line : lines) {
            var coordinates = line.getCoordinates();
            double angle = 0.0;
            for (int i = 0; i < coordinates.length - 1; ++i) {
                double dx = coordinates[i + 1].getX() - coordinates[i].getX();
                double dy = coordinates[i + 1].getY() - coordinates[i].getY();
                angle = Math.atan2(dy, dx);
                var point = GeoUtils.JTS_FACTORY.createPoint(coordinates[i]);
                map.put(point, angle);
            }
            var lastPoint = GeoUtils.JTS_FACTORY.createPoint(coordinates[coordinates.length - 1]);
            map.put(lastPoint, angle);
        }
        return map;
    }

    private List<LineString> magnetize(List<LineString> lines) {
        List<LineString> result = new ArrayList<>();

        int excludeRange = 50;
        STRtree strTree = getTree(lines);
        Set<Point> uniqueEndpoints = new HashSet<>(); // UniqueLineEndpoints.findUniqueEndpoints(lines);
        Map<Point, Double> pointToAngleMap = getPointToAngleMap(lines);
        for (var line : lines) {
            var coordinates = line.getCoordinates();
            List<Point> magnetizedPoints = new ArrayList<>();
            for (var i = 0; i < coordinates.length; i++) {
                Set<Point> excludedPoints = new HashSet<>();
                for (var ii = i - excludeRange; ii < i + excludeRange; ii++) {
                    if (ii < 0 || ii >= coordinates.length) {
                        continue;
                    }
                    Point point = GeoUtils.JTS_FACTORY.createPoint(coordinates[ii]);
                    excludedPoints.add(point);
                }
                Point queryPoint = GeoUtils.JTS_FACTORY.createPoint(coordinates[i]);
                if (uniqueEndpoints.contains(queryPoint)) {
                    magnetizedPoints.add(queryPoint);
                }
                else {
                    List<Point> closePoints = findClosePoints(strTree, queryPoint);
                    closePoints.removeAll(excludedPoints);
                    Map<Point, Double> weights = new HashMap<>();
                    double queryPointAngle = pointToAngleMap.get(queryPoint);
                    for (var closePoint : closePoints) {
                        double closePointAngle = pointToAngleMap.get(closePoint);
                        double angleDifference = queryPointAngle - closePointAngle;
                        double angleWeight = Math.abs(Math.cos(angleDifference));
                        double distance = queryPoint.distance(closePoint);
                        double distanceWeight = distance > 0.0 ? 1 / distance : 1;
                        weights.put(closePoint, distanceWeight * angleWeight);
                    }
                    var midpointOthers = closePoints.size() > 0 ? getMidpoint(closePoints, weights) : queryPoint;
                    var midpoint = getMidpoint(queryPoint, midpointOthers);
                    weights.clear();
                    weights.put(queryPoint, 0.05);
                    weights.put(midpoint, 0.95);
                    List<Point> points = new ArrayList<>();
                    points.add(queryPoint);
                    points.add(midpoint);
                    midpoint = getMidpoint(points, weights);
                    magnetizedPoints.add(midpoint);
                }
            }
            result.add(GeoUtils.JTS_FACTORY.createLineString(magnetizedPoints.stream().map(Point::getCoordinate).toArray(Coordinate[]::new)));
        }

        return result;
    }

    private List<LineString> iterate(List<LineString> lines) {
        List<LineString> densified = new ArrayList<>();
        for (var line : lines) {
            densified.add(LineStringDensifier.densify(line, densifyDistance));
        }

        // var merger = new LoopLineMerger();
        // merger.setPrecisionModel(new PrecisionModel());
        // for (var line : densified) {
        //     merger.add(line);
        // }
        // var merged = merger.getMergedLineStrings();

        var magnetized = magnetize(densified);
        return magnetized;

        // var merger = new LoopLineMerger();
        // merger.setPrecisionModel(new PrecisionModel());

        // for (var line : magnetized) {
        //     merger.add(line);
        // }

        // merger2.setLoopMinLength(loopMinLength);
        // merger2.setStubMinLength(loopMinLength);
        // merger2.setTolerance(tolernace);

        // return merger.getMergedLineStrings();
    }


    public List<LineString> getMagnetizedLineStrings() {
        List<LineString> lines = List.copyOf(input);
        var merger = new LoopLineMerger();
        merger.setPrecisionModel(new PrecisionModel());
        merger.setMergeStrokes(true);
        for (var line : lines) {
            merger.add(line);
        }
        lines = merger.getMergedLineStrings();
        for (int i = 0; i < iterations; ++i) {
            lines = iterate(lines);
        }
        merger = new LoopLineMerger();
        // merger.setPrecisionModel(new PrecisionModel(16));
        merger.setPrecisionModel(new PrecisionModel());
        for (var line : lines) {
            merger.add(line);
        }
        merger.setLoopMinLength(loopMinLength);
        merger.setStubMinLength(loopMinLength);
        merger.setTolerance(tolernace);
        return merger.getMergedLineStrings(); 
    }

    public List<LineString> getErasedLineStrings() {
        List<LineString> lines = List.copyOf(input);
        var merger = new LoopLineMerger();
        merger.setPrecisionModel(new PrecisionModel());
        // merger.setMergeStrokes(true);
        for (var line : lines) {
            merger.add(line);
        }
        lines = merger.getMergedLineStrings();
        lines.sort(Comparator.comparingDouble(LineString::getLength).reversed());
        List<LineString> densified = new ArrayList<>();
        for (var line : lines) {
            densified.add(LineStringDensifier.densify(line, densifyDistance));
        }
        lines = densified;
        STRtree strTree = getTree(lines);
        Set<Point> erasedPoints = new HashSet<>();
        for (var line : lines) {
            var coordinates = line.getCoordinates();
            Set<Point> excludedPoints = new HashSet<>();
            for (var coordinate : coordinates) {
                excludedPoints.add(GeoUtils.JTS_FACTORY.createPoint(coordinate));
            }
            for (var coordinate : coordinates) {
                Point point = GeoUtils.JTS_FACTORY.createPoint(coordinate);
                if (erasedPoints.contains(point)) {
                    continue;
                }
                List<Point> closePoints = findClosePoints(strTree, point);
                closePoints.removeAll(excludedPoints);
                erasedPoints.addAll(closePoints);
            }   
        }

        HashSet<Coordinate> erasedCoordinates = new HashSet<>();
        for (var erasedPoint : erasedPoints) {
            erasedCoordinates.add(erasedPoint.getCoordinate());
        }
        List<LineString> result = new ArrayList<>();
        for (var line : lines) {
            result.addAll(LineStringBreaker.breakLineString(line, erasedCoordinates, 10 * radius));
        }

        merger = new LoopLineMerger();
        // merger.setPrecisionModel(new PrecisionModel(16));
        merger.setPrecisionModel(new PrecisionModel());
        for (var line : result) {
            merger.add(line);
        }
        merger.setLoopMinLength(loopMinLength);
        merger.setStubMinLength(loopMinLength);
        merger.setTolerance(tolernace);
        return merger.getMergedLineStrings(); 
    }

    // you are given a List<LineString> lines and a double radius
    // first merge strokes based on angle at 3+-way-junctions
    // then sort the linestring by length, longest first
    // then loop over the linestrings and loop over the coordinates of each line string. 
    // for each coordinate, make a point
    // if the point is in the erased points, skip
    // else find all points that fall into the radius
    // filter those points for points that belong to other linestrings
    // add those points to a set of erased points
    // now the erased points set is completed


    // double radius = 5 * densifyDistance;
    // double loopMinLength = 5 * densifyDistance;
    // double tolerance = 0.5 * densifyDistance


    // public static void main(String[] args) {

    //     String filePath = "data/input.csv";
    //     double densifyDistance = 1e-6;

    //     List<LineString> lines = getInput(filePath);

    //     int iterations = 10;
    //     for (int i = 0; i < iterations; ++i) {
    //         lines = process(lines, densifyDistance);
    //     }

    //     var merger = new LoopLineMerger();
    //     merger.setPrecisionModel(new PrecisionModel());
    //     for (var line : lines) {
    //         merger.add(line);
    //     }
    //     merger.setTolerance(1);
    //     lines = merger.getMergedLineStrings();
        
    //     for (var line : lines) {
    //         System.out.println(line);
    //     }
    // }
}
