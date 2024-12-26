import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;

public class LineStringDensifier {
    public static LineString densify(LineString lineString, double distance) {
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance must be greater than 0");
        }

        GeometryFactory geometryFactory = lineString.getFactory();
        Coordinate[] coordinates = lineString.getCoordinates();
        CoordinateList densifiedCoordinates = new CoordinateList();

        for (int i = 0; i < coordinates.length - 1; i++) {
            Coordinate start = coordinates[i];
            Coordinate end = coordinates[i + 1];
            densifiedCoordinates.add(start);

            double segmentLength = start.distance(end);
            int numPoints = (int) Math.ceil(segmentLength / distance) - 1;

            for (int j = 1; j <= numPoints; j++) {
                double fraction = j * distance / segmentLength;
                double x = start.x + fraction * (end.x - start.x);
                double y = start.y + fraction * (end.y - start.y);
                densifiedCoordinates.add(new Coordinate(x, y), false);
            }
        }
        densifiedCoordinates.add(coordinates[coordinates.length - 1]);

        return geometryFactory.createLineString(densifiedCoordinates.toCoordinateArray());
    }

    public static void main(String[] args) {
        GeometryFactory geometryFactory = new GeometryFactory();
        LineString lineString = geometryFactory.createLineString(new Coordinate[]{
            new Coordinate(0, 0),
            new Coordinate(10, 0)
        });

        double densifyDistance = 1.5;
        LineString densified = densify(lineString, densifyDistance);
        System.out.println("Original: " + lineString);
        System.out.println("Densified: " + densified);
    }
}
