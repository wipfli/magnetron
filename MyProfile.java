import java.nio.file.Path;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
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
            .withDefault("minzoom", 5)
            .withDefault("maxzoom", 14);
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
            features.line("magnetized")
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

        double tolerance = zoom == 14 ? 0 : 8 * 0.0625;
        if (layer.equals("orig") || zoom > 9) {
            return FeatureMerge.mergeLineStrings(items, 4 * 0.0625, tolerance, 4, true);
        }
        // densifyDistance 1e-6
        // z6, num tiles horizontally = 2 ** 6 = 64
        // 256 pixels per tile, pixels horizontally = 256 * 64 = 16384
        // z0, num tiles horizontally = 1
        // 256 pixels per tile, pixels horizontally = 256
        // 1e-6 on 0 to 1 on the z0 tile is, 1e-6 * 256 pixels at zoom 0 = 0.000256
        // is 64 * 0.000256 at zoom 6 = 0.016384
        // comparison: 0.25 * 0.0625 = 0.015625, basically the same!

        double densifyDistance = Math.pow(2, zoom - 14) * 8;
        return MyFeatureMerge.magnetron(items, densifyDistance, tolerance);
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