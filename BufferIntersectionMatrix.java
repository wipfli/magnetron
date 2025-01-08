import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Geometry;
import com.onthegomap.planetiler.geo.GeoUtils;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;


import java.util.ArrayList;
import java.util.List;

public class BufferIntersectionMatrix {

    public static double[][] computeIntersectionMatrix(List<LineString> lineStrings, double radius) {
        int size = lineStrings.size();
        double[][] intersectionMatrix = new double[size][size];

        List<Geometry> bufferedGeometries = new ArrayList<>();

        // Buffer each LineString by the given radius
        for (LineString line : lineStrings) {
            bufferedGeometries.add(BufferOp.bufferOp(line, radius));
        }

        // Compute intersection areas between all pairs
        for (int i = 0; i < size; i++) {
            for (int j = i; j < size; j++) {
                if (i == j) {
                    // Diagonal elements (self-intersection areas)
                    intersectionMatrix[i][j] = bufferedGeometries.get(i).getArea();
                } else {
                    // Intersection area of different geometries
                    Geometry intersection = bufferedGeometries.get(i).intersection(bufferedGeometries.get(j));
                    intersectionMatrix[i][j] = intersection.getArea();
                    intersectionMatrix[j][i] = intersectionMatrix[i][j]; // Symmetric matrix
                }
            }
        }

        return intersectionMatrix;
    }

    public static void main(String[] args) {
        GeometryFactory geometryFactory = GeoUtils.JTS_FACTORY;

        // Example LineStrings
        LineString line1 = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(10, 0)
        });

        LineString line2 = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(5, -5),
                new Coordinate(5, 5)
        });

        LineString line3 = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(5, 0),
                new Coordinate(20, 0)
        });

        List<LineString> lines = List.of(line1, line2, line3);
        double radius =4.0;

        double[][] intersectionMatrix = computeIntersectionMatrix(lines, radius);

        // Print the matrix
        for (double[] row : intersectionMatrix) {
            for (double value : row) {
                System.out.printf("%.2f ", value);
            }
            System.out.println();
        }
    }
}
