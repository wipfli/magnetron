import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.LineString;

import com.onthegomap.planetiler.geo.GeoUtils;

public class Run {
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

    public static void main(String[] args) {

        String filePath = "data/input.csv";

        List<LineString> lines = getInput(filePath);

        Magnetron magnetron = new Magnetron();

        for (int i = 0; i < 4; i++) {
            magnetron = new Magnetron();
            magnetron.setDensifyDistance(1e-6);
            magnetron.setLoopMinLength(0e-6); // 5e-6
            magnetron.setRadius(10e-6);
            magnetron.setTolerance(1e-7); // 1e-7
            magnetron.setIterations(1);
            for (var line : lines) {
                magnetron.add(line);
            }
            lines = magnetron.getMagnetizedLineStrings();
        }
        magnetron = new Magnetron();
        magnetron.setDensifyDistance(5e-7);
        magnetron.setRadius(1e-6);
        magnetron.setEraseMinLength(10e-6);
        magnetron.setLoopMinLength(15e-6);
        magnetron.setTolerance(1e-7); // 1e-7
        for (var line : lines) {
            magnetron.add(line);
        }
        lines = magnetron.getErasedLineStrings();
        
        for (var line : lines) {
            System.out.println(line);
        }
    }
}
