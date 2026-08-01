package io.saha.yoga.vision;

import io.saha.yoga.domain.*;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

public final class DemoLandmarkSource implements LandmarkSource {
    private long frame;
    private String poseId = "mountain";
    private EnumMap<LandmarkName, Landmark> displayed;
    private EnumMap<LandmarkName, Landmark> transitionFrom;
    private EnumMap<LandmarkName, Landmark> transitionWaypoint;
    private EnumMap<LandmarkName, Landmark> target;
    private long transitionStarted;
    private FaceDirection facing = FaceDirection.FRONT;
    @Override public void selectPose(String poseId) {
        String previousPose = this.poseId;
        transitionFrom = displayed == null ? build(this.poseId) : new EnumMap<>(displayed);
        this.poseId = poseId;
        facing = facingFor(poseId);
        target = build(poseId);
        transitionWaypoint = crossesFloorBoundary(previousPose,poseId) ? forwardFold() : null;
        transitionStarted = System.nanoTime();
    }
    @Override public LandmarkFrame nextFrame() {
        if (target == null) target = build(poseId);
        double progress = Math.min(1, (System.nanoTime()-transitionStarted)/5_000_000_000.0);
        if (transitionWaypoint == null) {
            double eased=progress*progress*(3-2*progress);
            displayed=interpolate(transitionFrom==null?target:transitionFrom,target,eased);
        } else if (progress<.5) {
            double part=progress*2; displayed=interpolate(transitionFrom,transitionWaypoint,part*part*(3-2*part));
        } else {
            double part=(progress-.5)*2; displayed=interpolate(transitionWaypoint,target,part*part*(3-2*part));
        }
        constrain(displayed, facing);
        ground(displayed);
        frame++;
        return new LandmarkFrame(Instant.now(), displayed);
    }
    private EnumMap<LandmarkName, Landmark> build(String id) {
        var points = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        standing(points);
        switch (id) {
            case "chair" -> chair(points);
            case "warrior_one" -> warriorOne(points);
            case "warrior_two" -> warriorTwo(points);
            case "triangle" -> triangle(points);
            case "tree" -> tree(points);
            case "cat_cow" -> tabletop(points, false);
            case "bird_dog" -> tabletop(points, true);
            case "low_lunge" -> lowLunge(points);
            case "bridge" -> bridge(points);
            case "seated_fold" -> seatedFold(points);
            case "rest" -> rest(points);
            default -> { }
        }
        constrain(points, facingFor(id));
        ground(points);
        return points;
    }

    /**
     * Rest the figure on the floor reference line.
     *
     * constrain() rebuilds every limb outward from the hips at fixed bone
     * lengths, so a pose's authored foot and hand positions do not survive it:
     * each limb ends wherever its chain reaches. Nothing then relates the body
     * to the floor the view draws at a fixed height, so each pose floated above
     * it or sank through it by its own arbitrary amount.
     *
     * Shifting every point by the same amount fixes that without touching the
     * pose itself: a translation preserves every bone length, angle and
     * left/right relationship, so the only thing that changes is how high the
     * body sits. The shift is taken from the LOWEST point, which is the one
     * bearing weight -- the standing foot in tree, the supporting hand and knee
     * in bird dog -- so the parts that should be on the floor land on it and
     * nothing is left hanging through it.
     */
    private void ground(EnumMap<LandmarkName, Landmark> p) {
        if (p.isEmpty()) return;
        double lowest = p.values().stream().mapToDouble(Landmark::y).max().orElse(LandmarkSource.FLOOR_Y);
        double shift = LandmarkSource.FLOOR_Y - lowest;
        p.replaceAll((name, mark) -> new Landmark(mark.x(), mark.y() + shift, mark.confidence()));
    }
    private EnumMap<LandmarkName, Landmark> interpolate(Map<LandmarkName, Landmark> from, Map<LandmarkName, Landmark> to, double amount) {
        var result = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        to.forEach((name,end) -> { var start=from.getOrDefault(name,end); result.put(name,new Landmark(start.x()+(end.x()-start.x())*amount,start.y()+(end.y()-start.y())*amount,confidence())); });
        return result;
    }
    @Override public boolean isTransitioning() { return target != null && (System.nanoTime()-transitionStarted) < 5_000_000_000L; }
    @Override public FaceDirection faceDirection() { return facing; }
    LandmarkFrame targetFrame() { return new LandmarkFrame(Instant.now(), target == null ? build(poseId) : target); }
    private FaceDirection facingFor(String id) { return switch(id) {
        case "warrior_two" -> FaceDirection.LEFT;
        case "cat_cow", "bird_dog" -> FaceDirection.DOWN;
        case "triangle" -> FaceDirection.UP;
        case "chair" -> FaceDirection.RIGHT;
        case "seated_fold" -> FaceDirection.RIGHT;
        case "bridge", "rest" -> FaceDirection.UP;
        case "low_lunge" -> FaceDirection.LEFT;
        default -> FaceDirection.FRONT;
    }; }
    @Override public String transitionGuidance() { return transitionWaypoint == null
            ? "Move slowly while keeping each joint comfortable."
            : "Soften your knees, fold forward, place your hands down, then move one knee at a time."; }
    @Override public double spineBend() { return poseId.equals("cat_cow") ? Math.sin(System.nanoTime()/1_200_000_000.0)*.075 : 0; }
    private boolean crossesFloorBoundary(String from,String to) { return isFloor(from)!=isFloor(to); }
    private boolean isFloor(String id) { return switch(id){case "cat_cow","bird_dog","bridge","seated_fold","rest"->true;default->false;}; }
    private EnumMap<LandmarkName, Landmark> forwardFold() {
        var p=new EnumMap<LandmarkName,Landmark>(LandmarkName.class); standing(p);
        at(p,LandmarkName.NOSE,.48,.57); shoulders(p,.47,.51,.53,.52); hips(p,.46,.46,.54,.47);
        arms(p,.44,.64,.43,.77,.56,.65,.57,.78); at(p,LandmarkName.LEFT_HAND,.43,.85);at(p,LandmarkName.RIGHT_HAND,.57,.85);
        legs(p,.45,.67,.44,.87,.55,.68,.56,.87);toes(p,.44,.94,.56,.94);constrain(p,FaceDirection.DOWN);return p;
    }

    private void constrain(EnumMap<LandmarkName, Landmark> p, FaceDirection view) {
        var raw = new EnumMap<>(p);
        boolean side = view != FaceDirection.FRONT;
        var hipCenter = midpoint(raw.get(LandmarkName.LEFT_HIP),raw.get(LandmarkName.RIGHT_HIP));
        var shoulderCenterRaw = midpoint(raw.get(LandmarkName.LEFT_SHOULDER),raw.get(LandmarkName.RIGHT_SHOULDER));
        var shoulderCenter = extend(hipCenter,shoulderCenterRaw,.24);
        placePair(p,LandmarkName.LEFT_HIP,LandmarkName.RIGHT_HIP,hipCenter,raw.get(LandmarkName.LEFT_HIP),raw.get(LandmarkName.RIGHT_HIP),side?.04:.10);
        placePair(p,LandmarkName.LEFT_SHOULDER,LandmarkName.RIGHT_SHOULDER,shoulderCenter,raw.get(LandmarkName.LEFT_SHOULDER),raw.get(LandmarkName.RIGHT_SHOULDER),side?.05:.17);
        chain(p,raw,LandmarkName.LEFT_SHOULDER,LandmarkName.LEFT_ELBOW,LandmarkName.LEFT_WRIST,LandmarkName.LEFT_HAND,.15,.14,.055);
        chain(p,raw,LandmarkName.RIGHT_SHOULDER,LandmarkName.RIGHT_ELBOW,LandmarkName.RIGHT_WRIST,LandmarkName.RIGHT_HAND,.15,.14,.055);
        chain(p,raw,LandmarkName.LEFT_HIP,LandmarkName.LEFT_KNEE,LandmarkName.LEFT_ANKLE,LandmarkName.LEFT_TOE,.23,.22,.08);
        chain(p,raw,LandmarkName.RIGHT_HIP,LandmarkName.RIGHT_KNEE,LandmarkName.RIGHT_ANKLE,LandmarkName.RIGHT_TOE,.23,.22,.08);
        p.put(LandmarkName.NOSE,withConfidence(extend(shoulderCenter,raw.get(LandmarkName.NOSE),.13)));
    }
    private void placePair(EnumMap<LandmarkName, Landmark> p,LandmarkName left,LandmarkName right,Landmark center,Landmark rawLeft,Landmark rawRight,double width){
        double dx=rawRight.x()-rawLeft.x(),dy=rawRight.y()-rawLeft.y(),length=Math.max(.001,Math.hypot(dx,dy));
        p.put(left,new Landmark(center.x()-dx/length*width/2,center.y()-dy/length*width/2,confidence()));
        p.put(right,new Landmark(center.x()+dx/length*width/2,center.y()+dy/length*width/2,confidence()));
    }
    private void chain(EnumMap<LandmarkName, Landmark> p,Map<LandmarkName, Landmark> raw,LandmarkName root,LandmarkName joint,LandmarkName end,LandmarkName tip,double first,double second,double third){
        var a=p.get(root); var b=extend(a,raw.get(joint),first); var c=extend(b,raw.get(end),second); var d=extend(c,raw.get(tip),third);
        p.put(joint,withConfidence(b));p.put(end,withConfidence(c));p.put(tip,withConfidence(d));
    }
    private Landmark midpoint(Landmark a,Landmark b){return new Landmark((a.x()+b.x())/2,(a.y()+b.y())/2,confidence());}
    private Landmark extend(Landmark from,Landmark toward,double distance){double dx=toward.x()-from.x(),dy=toward.y()-from.y(),length=Math.max(.001,Math.hypot(dx,dy));return new Landmark(from.x()+dx/length*distance,from.y()+dy/length*distance,confidence());}
    private Landmark withConfidence(Landmark p){return new Landmark(p.x(),p.y(),confidence());}
    private double confidence(){return frame%300>=290?.55:.94;}
    private void standing(EnumMap<LandmarkName, Landmark> p) {
        at(p, LandmarkName.NOSE,.50,.10); shoulders(p,.42,.21,.58,.21); arms(p,.40,.40,.39,.58,.60,.40,.61,.58);
        hips(p,.46,.48,.54,.48); legs(p,.46,.70,.45,.88,.54,.70,.55,.88); toes(p,.45,.96,.55,.96);
    }
    private void chair(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.38,.16); shoulders(p,.40,.29,.43,.30); arms(p,.31,.15,.33,.02,.34,.15,.36,.02);
        hips(p,.52,.51,.55,.52); legs(p,.69,.54,.67,.78,.72,.57,.70,.81); toes(p,.74,.78,.77,.81);
    }
    private void warriorOne(EnumMap<LandmarkName, Landmark> p) {
        arms(p,.34,.13,.44,.04,.66,.13,.56,.04); hips(p,.46,.48,.54,.48);
        legs(p,.34,.65,.34,.86,.67,.65,.79,.85); toes(p,.25,.86,.87,.85);
    }
    private void warriorTwo(EnumMap<LandmarkName, Landmark> p) {
        arms(p,.29,.21,.16,.21,.71,.21,.84,.21); hips(p,.46,.48,.54,.48);
        legs(p,.34,.64,.34,.85,.67,.66,.80,.85); toes(p,.25,.85,.80,.77);
    }
    private void triangle(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.29,.24); shoulders(p,.32,.35,.39,.31);
        arms(p,.34,.49,.34,.63,.45,.20,.50,.08); hips(p,.45,.53,.53,.53);
        legs(p,.34,.69,.22,.86,.65,.69,.78,.86); toes(p,.14,.86,.86,.86);
    }
    private void tree(EnumMap<LandmarkName, Landmark> p) {
        arms(p,.30,.12,.42,.01,.70,.12,.58,.01); hips(p,.46,.48,.54,.48);
        legs(p,.47,.69,.47,.88,.68,.62,.50,.57); toes(p,.53,.89,.52,.64);
    }
    private void tabletop(EnumMap<LandmarkName, Landmark> p, boolean extended) {
        at(p,LandmarkName.NOSE,.23,.40); shoulders(p,.34,.45,.36,.48); hips(p,.58,.45,.60,.48);
        if (extended) {
            arms(p,.23,.41,.11,.38,.36,.62,.36,.79);
            at(p,LandmarkName.LEFT_HAND,.05,.36); at(p,LandmarkName.RIGHT_HAND,.30,.79);
            legs(p,.58,.62,.58,.79,.71,.41,.84,.37);
            toes(p,.64,.79,.91,.35);
        } else {
            arms(p,.34,.62,.34,.79,.37,.65,.37,.82);
            at(p,LandmarkName.LEFT_HAND,.28,.79); at(p,LandmarkName.RIGHT_HAND,.31,.82);
            legs(p,.58,.62,.58,.79,.61,.65,.61,.82);
            toes(p,.65,.80,.68,.82);
        }
    }
    private void lowLunge(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.49,.15); shoulders(p,.43,.27,.55,.27); arms(p,.35,.13,.45,.02,.63,.13,.53,.02);
        hips(p,.46,.51,.54,.51); legs(p,.31,.65,.31,.85,.68,.70,.84,.83); toes(p,.22,.85,.91,.83);
    }
    private void bridge(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.18,.67); shoulders(p,.28,.70,.30,.73); arms(p,.40,.77,.53,.81,.42,.79,.55,.83);
        hips(p,.53,.52,.56,.54); legs(p,.68,.62,.73,.81,.71,.64,.77,.82); toes(p,.80,.81,.84,.82);
    }
    private void seatedFold(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.61,.45); shoulders(p,.53,.52,.56,.54); arms(p,.66,.59,.76,.66,.69,.61,.79,.68);
        hips(p,.39,.66,.42,.69); legs(p,.58,.69,.77,.70,.61,.73,.80,.74); toes(p,.84,.69,.87,.73);
    }
    private void rest(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.17,.67); shoulders(p,.28,.70,.32,.73); arms(p,.41,.63,.54,.58,.43,.80,.56,.85);
        hips(p,.53,.70,.57,.73); legs(p,.70,.68,.84,.64,.72,.77,.86,.82); toes(p,.90,.63,.92,.82);
    }
    private void shoulders(EnumMap<LandmarkName, Landmark> p,double lx,double ly,double rx,double ry){at(p,LandmarkName.LEFT_SHOULDER,lx,ly);at(p,LandmarkName.RIGHT_SHOULDER,rx,ry);}
    private void hips(EnumMap<LandmarkName, Landmark> p,double lx,double ly,double rx,double ry){at(p,LandmarkName.LEFT_HIP,lx,ly);at(p,LandmarkName.RIGHT_HIP,rx,ry);}
    private void arms(EnumMap<LandmarkName, Landmark> p,double lex,double ley,double lwx,double lwy,double rex,double rey,double rwx,double rwy){
        at(p,LandmarkName.LEFT_ELBOW,lex,ley); at(p,LandmarkName.LEFT_WRIST,lwx,lwy);
        at(p,LandmarkName.RIGHT_ELBOW,rex,rey); at(p,LandmarkName.RIGHT_WRIST,rwx,rwy);
        handBeyond(p, LandmarkName.LEFT_HAND, lex, ley, lwx, lwy);
        handBeyond(p, LandmarkName.RIGHT_HAND, rex, rey, rwx, rwy);
    }
    private void handBeyond(EnumMap<LandmarkName, Landmark> p, LandmarkName hand, double elbowX, double elbowY, double wristX, double wristY) {
        double dx=wristX-elbowX, dy=wristY-elbowY, length=Math.max(.001,Math.hypot(dx,dy));
        at(p,hand,wristX+(dx/length)*.055,wristY+(dy/length)*.055);
    }
    private void legs(EnumMap<LandmarkName, Landmark> p,double lkx,double lky,double lax,double lay,double rkx,double rky,double rax,double ray){at(p,LandmarkName.LEFT_KNEE,lkx,lky);at(p,LandmarkName.LEFT_ANKLE,lax,lay);at(p,LandmarkName.RIGHT_KNEE,rkx,rky);at(p,LandmarkName.RIGHT_ANKLE,rax,ray);}
    private void toes(EnumMap<LandmarkName, Landmark> p,double lx,double ly,double rx,double ry){at(p,LandmarkName.LEFT_TOE,lx,ly);at(p,LandmarkName.RIGHT_TOE,rx,ry);}
    private void at(EnumMap<LandmarkName, Landmark> p, LandmarkName name,double x,double y){ put(p,name,x,y); }
    private void put(EnumMap<LandmarkName, Landmark> map, LandmarkName name, double x, double y) {
        map.put(name, new Landmark(x, y, confidence()));
    }
    @Override public String description() { return "Observed landmarks · synthetic demo · not instruction"; }
}
