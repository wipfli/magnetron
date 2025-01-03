import java.util.List;
import java.util.Set;
import java.util.ArrayList;

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

    private List<LineString> magnetize(List<LineString> lines) {
        List<LineString> result = new ArrayList<>();

        STRtree strTree = getTree(lines);
        Set<Point> uniqueEndpoints = UniqueLineEndpoints.findUniqueEndpoints(lines);
        for (var line : lines) {
            var coordinates = line.getCoordinates();
            List<Point> magnetizedPoints = new ArrayList<>();
            for (var coordinate : coordinates) {
                Point queryPoint = GeoUtils.JTS_FACTORY.createPoint(coordinate);
                if (uniqueEndpoints.contains(queryPoint)) {
                    magnetizedPoints.add(queryPoint);
                }
                else {
                    List<Point> closePoints = findClosePoints(strTree, queryPoint);
                    magnetizedPoints.add(getMidpoint(closePoints));
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

        var merger = new LoopLineMerger();
        merger.setPrecisionModel(new PrecisionModel());
        for (var line : densified) {
            merger.add(line);
        }
        var merged = merger.getMergedLineStrings();

        var magnetized = magnetize(merged);

        var merger2 = new LoopLineMerger();
        merger2.setPrecisionModel(new PrecisionModel(16));

        for (var line : magnetized) {
            merger2.add(line);
        }

        // merger2.setLoopMinLength(loopMinLength);
        // merger2.setStubMinLength(loopMinLength);
        // merger2.setTolerance(tolernace);

        return merger2.getMergedLineStrings();
    }


    public List<LineString> getMagnetizedLineStrings() {
        List<LineString> lines = List.copyOf(input);
        for (int i = 0; i < iterations; ++i) {
            lines = iterate(lines);
        }
        var merger = new LoopLineMerger();
        merger.setPrecisionModel(new PrecisionModel());

        for (var line : lines) {
            merger.add(line);
        }

        merger.setLoopMinLength(loopMinLength);
        merger.setStubMinLength(loopMinLength);
        merger.setTolerance(tolernace);

        return merger.getMergedLineStrings(); 
    }

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
