import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Coordinate;

import com.onthegomap.planetiler.geo.GeoUtils;
import com.onthegomap.planetiler.util.LoopLineMerger;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

public class Magnetron {

    static LineString linestring(double startLat, double startLon, double endLat, double endLon) {
        var coordinates = new Coordinate[2];
        coordinates[0] = new CoordinateXY(startLon, startLat);
        coordinates[1] = new CoordinateXY(endLon, endLat);
        return GeoUtils.JTS_FACTORY.createLineString(coordinates);
    }

    public static void main(String[] args) {
        String filePath = "data/input.csv";

        String line;
        String delimiter = ",";
        var merger = new LoopLineMerger();
        merger.setPrecisionModel(new PrecisionModel());

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                String[] values = line.split(delimiter);

                double startLon = Double.parseDouble(values[0]);
                double startLat = Double.parseDouble(values[1]);
                double endLon = Double.parseDouble(values[2]);
                double endLat = Double.parseDouble(values[3]);

                merger.add(linestring(startLat, startLon, endLat, endLon));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // merger.setLoopMinLength(0.01);
        // merger.setStubMinLength(0.01);
        // merger.setTolerance(0.0001);

        for (var linestring : merger.getMergedLineStrings()) {
            System.out.println(linestring);
        }
    }
}
