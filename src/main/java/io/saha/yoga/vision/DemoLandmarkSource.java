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
    private EnumMap<LandmarkName, Landmark> target;
    private long transitionStarted;
    private FaceDirection facing = FaceDirection.FRONT;
    @Override public void selectPose(String poseId) {
        transitionFrom = displayed == null ? build(this.poseId) : new EnumMap<>(displayed);
        this.poseId = poseId;
        facing = facingFor(poseId);
        target = build(poseId);
        transitionStarted = System.nanoTime();
    }
    @Override public LandmarkFrame nextFrame() {
        if (target == null) target = build(poseId);
        double progress = Math.min(1, (System.nanoTime()-transitionStarted)/3_000_000_000.0);
        double eased = progress*progress*(3-2*progress);
        displayed = interpolate(transitionFrom == null ? target : transitionFrom, target, eased);
        constrain(displayed, facing);
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
        return points;
    }
    private EnumMap<LandmarkName, Landmark> interpolate(Map<LandmarkName, Landmark> from, Map<LandmarkName, Landmark> to, double amount) {
        var result = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        to.forEach((name,end) -> { var start=from.getOrDefault(name,end); result.put(name,new Landmark(start.x()+(end.x()-start.x())*amount,start.y()+(end.y()-start.y())*amount,confidence())); });
        return result;
    }
    @Override public boolean isTransitioning() { return target != null && (System.nanoTime()-transitionStarted) < 3_000_000_000L; }
    @Override public FaceDirection faceDirection() { return facing; }
    LandmarkFrame targetFrame() { return new LandmarkFrame(Instant.now(), target == null ? build(poseId) : target); }
    private FaceDirection facingFor(String id) { return switch(id) {
        case "warrior_two", "triangle", "cat_cow", "bird_dog", "chair" -> FaceDirection.LEFT;
        case "seated_fold" -> FaceDirection.RIGHT;
        case "bridge", "rest" -> FaceDirection.UP;
        case "low_lunge" -> FaceDirection.LEFT;
        default -> FaceDirection.FRONT;
    }; }

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
        hips(p,.46,.48,.54,.48); legs(p,.46,.70,.45,.88,.54,.70,.55,.88); toes(p,.39,.89,.61,.89);
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
        legs(p,.34,.64,.34,.85,.67,.66,.80,.85); toes(p,.25,.85,.88,.85);
    }
    private void triangle(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.29,.24); shoulders(p,.32,.35,.39,.31);
        arms(p,.30,.49,.29,.63,.45,.20,.50,.08); hips(p,.45,.53,.53,.53);
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
            legs(p,.58,.62,.58,.79,.71,.41,.84,.37);
            toes(p,.64,.79,.91,.35);
        } else {
            arms(p,.34,.62,.34,.79,.37,.65,.37,.82);
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
    @Override public String description() { return "Demo landmarks · " + poseId.replace('_', ' '); }
}
