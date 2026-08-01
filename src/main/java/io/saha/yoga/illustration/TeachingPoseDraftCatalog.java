package io.saha.yoga.illustration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static io.saha.yoga.illustration.BodyAnchor.*;

public final class TeachingPoseDraftCatalog {
    private static final double FLOOR = .90;
    private final Map<String, TeachingPoseDraft> drafts = Map.of(
            "chair", chair(),
            "warrior_one", warriorOne(),
            "warrior_two", warriorTwo()
    );

    public List<TeachingPoseDraft> all() { return List.of(drafts.get("chair"), drafts.get("warrior_one"), drafts.get("warrior_two")); }
    public TeachingPoseDraft require(String id) {
        var draft = drafts.get(id);
        if (draft == null) throw new IllegalArgumentException("No teaching draft for " + id);
        return draft;
    }

    private static TeachingPoseDraft chair() {
        var p = points();
        at(p, HEAD,.46,.18); at(p,NECK,.45,.27); at(p,SHOULDER,.45,.33); at(p,HIP,.34,.55);
        at(p,FRONT_ELBOW,.49,.19); at(p,FRONT_HAND,.51,.07); at(p,REAR_ELBOW,.44,.18); at(p,REAR_HAND,.46,.06);
        at(p,FRONT_KNEE,.57,.67); at(p,FRONT_ANKLE,.58,.87); at(p,FRONT_HEEL,.54,FLOOR); at(p,FRONT_TOE,.67,FLOOR);
        at(p,REAR_KNEE,.53,.68); at(p,REAR_ANKLE,.54,.87); at(p,REAR_HEEL,.50,FLOOR); at(p,REAR_TOE,.62,FLOOR);
        return new TeachingPoseDraft("chair","Chair","Side view","Forward",FLOOR,p);
    }

    private static TeachingPoseDraft warriorOne() {
        var p = points();
        at(p,HEAD,.50,.17); at(p,NECK,.49,.27); at(p,SHOULDER,.49,.32); at(p,HIP,.47,.53);
        at(p,FRONT_ELBOW,.54,.18); at(p,FRONT_HAND,.53,.06); at(p,REAR_ELBOW,.45,.18); at(p,REAR_HAND,.47,.06);
        at(p,FRONT_KNEE,.68,.65); at(p,FRONT_ANKLE,.68,.87); at(p,FRONT_HEEL,.64,FLOOR); at(p,FRONT_TOE,.80,FLOOR);
        at(p,REAR_KNEE,.31,.70); at(p,REAR_ANKLE,.19,.87); at(p,REAR_HEEL,.16,FLOOR); at(p,REAR_TOE,.31,FLOOR);
        return new TeachingPoseDraft("warrior_one","Warrior I","Three-quarter view","Forward",FLOOR,p);
    }

    private static TeachingPoseDraft warriorTwo() {
        var p = points();
        at(p,HEAD,.52,.18); at(p,NECK,.49,.27); at(p,SHOULDER,.48,.32); at(p,HIP,.47,.53);
        at(p,FRONT_ELBOW,.66,.31); at(p,FRONT_HAND,.83,.31); at(p,REAR_ELBOW,.31,.31); at(p,REAR_HAND,.14,.31);
        at(p,FRONT_KNEE,.68,.65); at(p,FRONT_ANKLE,.68,.87); at(p,FRONT_HEEL,.64,FLOOR); at(p,FRONT_TOE,.81,FLOOR);
        at(p,REAR_KNEE,.32,.69); at(p,REAR_ANKLE,.18,.87); at(p,REAR_HEEL,.15,FLOOR); at(p,REAR_TOE,.30,FLOOR);
        return new TeachingPoseDraft("warrior_two","Warrior II","Three-quarter view","Over front hand",FLOOR,p);
    }

    private static EnumMap<BodyAnchor, IllustrationPoint> points() { return new EnumMap<>(BodyAnchor.class); }
    private static void at(EnumMap<BodyAnchor, IllustrationPoint> points, BodyAnchor anchor, double x, double y) {
        points.put(anchor, new IllustrationPoint(x,y));
    }
}
