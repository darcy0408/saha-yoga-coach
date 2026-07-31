package io.saha.yoga.vision;

import io.saha.yoga.domain.*;
import java.time.Instant;
import java.util.EnumMap;

public final class DemoLandmarkSource implements LandmarkSource {
    private long frame;
    @Override public LandmarkFrame nextFrame() {
        var points = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        put(points, LandmarkName.NOSE, .50, .08);
        put(points, LandmarkName.LEFT_SHOULDER, .42, .22); put(points, LandmarkName.RIGHT_SHOULDER, .58, .22);
        put(points, LandmarkName.LEFT_ELBOW, .30, .25); put(points, LandmarkName.RIGHT_ELBOW, .70, .25);
        put(points, LandmarkName.LEFT_WRIST, .18, .25); put(points, LandmarkName.RIGHT_WRIST, .82, .25);
        put(points, LandmarkName.LEFT_HIP, .45, .48); put(points, LandmarkName.RIGHT_HIP, .55, .48);
        put(points, LandmarkName.LEFT_KNEE, .36, .68); put(points, LandmarkName.RIGHT_KNEE, .61, .70);
        put(points, LandmarkName.LEFT_ANKLE, .25, .90); put(points, LandmarkName.RIGHT_ANKLE, .68, .90);
        frame++;
        return new LandmarkFrame(Instant.now(), points);
    }
    private void put(EnumMap<LandmarkName, Landmark> map, LandmarkName name, double x, double y) {
        map.put(name, new Landmark(x, y, frame % 30 == 29 ? .55 : .94));
    }
    @Override public String description() { return "Camera-free synthetic landmark demonstration"; }
}

