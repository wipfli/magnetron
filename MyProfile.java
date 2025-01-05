import java.nio.file.Path;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.List;

public class MyProfile implements Profile {
    public static void main(String[] args) {
        var arguments = Arguments.fromArgs(args)
            .withDefault("download", true)
            .withDefault("minzoom", 6)
            .withDefault("maxzoom",14);
        String area = "massachusetts";
        Planetiler.create(arguments)
            .addOsmSource("osm", Path.of("data", area + ".osm.pbf"), "geofabrik:" + area)
            .overwriteOutput(Path.of("data", "magnetized.pmtiles"))
            .setProfile(new MyProfile())
            .run();
    }


    @Override
    public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {
        if (sourceFeature.canBeLine() && sourceFeature.hasTag("highway", "motorway")) {
            features.line("lines")
                .setPixelTolerance(0.0)
                .setMinPixelSize(0.0);
            // features.line("orig")
            //     .setPixelTolerance(0.0)
            //     .setMinPixelSize(0.0);
        }
    }

    @Override
    public List<VectorTile.Feature> postProcessLayerFeatures(String layer, int zoom,
            List<VectorTile.Feature> items) throws GeometryException {
        if (layer.equals("orig")) { 
            return items;
        }
        double densifyDistance = 0.25 * 0.0625; //* Math.sqrt(2);
        double loopMinLength = 8 * 0.0625;
        double radius = 4 * 0.0625;
        double tolerance = 8 * 0.0625;
        if (zoom == 10) {
            tolerance = 4 * 0.0625;
        }
        if (zoom > 10) {
            tolerance = 0.0;
        }
        int iterations = 2;
        if (zoom == 9) {
            iterations = 1;
        }
        if (zoom > 9) {
            iterations = 0;
        }
        return MyFeatureMerge.magnetron(items, densifyDistance, loopMinLength, radius, tolerance, iterations);
    }

    @Override
    public String attribution() {
        return OSM_ATTRIBUTION;
    }

    @Override
    public boolean isOverlay() {
        return true;
    }
}