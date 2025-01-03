import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.ArrayList;

public class UniqueLineEndpoints {

    public static Set<Point> findUniqueEndpoints(List<LineString> lineStrings) {
        Map<Point, Integer> pointCount = new HashMap<>();
        for (LineString line : lineStrings) {
            Point startPoint = line.getStartPoint();
            Point endPoint = line.getEndPoint();

            pointCount.put(startPoint, pointCount.getOrDefault(startPoint, 0) + 1);
            pointCount.put(endPoint, pointCount.getOrDefault(endPoint, 0) + 1);
        }
        Set<Point> uniqueEndpoints = new HashSet<>();
        for (Map.Entry<Point, Integer> entry : pointCount.entrySet()) {
            if (entry.getValue() == 1) {
                uniqueEndpoints.add(entry.getKey());
            }
        }
        return uniqueEndpoints;
    }

    public static void main(String[] args) {
        List<LineString> lines = new ArrayList<>();
        // Populate 'lines' with LineString instances

        Set<Point> uniqueEndpoints = findUniqueEndpoints(lines);
        
        // Print the unique points
        for (Point p : uniqueEndpoints) {
            System.out.println(p);
        }
    }
}
