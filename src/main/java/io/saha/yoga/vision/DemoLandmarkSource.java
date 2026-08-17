package io.saha.yoga.vision;

import io.saha.yoga.domain.*;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

public final class DemoLandmarkSource implements LandmarkSource {
    private long frame;
    private String poseId = "easy_seat";
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
            case "cat_cow" -> tabletop(points);
            case "low_lunge" -> lowLunge(points);
            case "bridge" -> bridge(points);
            case "seated_fold" -> seatedFold(points);
            case "rest" -> rest(points);
            case "easy_seat" -> easySeat(points, false);
            case "seated_side_reach" -> easySeat(points, true);
            case "seated_twist" -> easySeat(points, false);
            case "head_to_knee" -> headToKnee(points);
            case "upward_salute" -> upwardSalute(points);
            case "standing_fold" -> standingFold(points);
            case "downward_dog" -> downwardDog(points);
            case "goddess" -> goddess(points);
            case "plank" -> plank(points);
            case "locust" -> locust(points);
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
    public LandmarkFrame targetFrame() { return new LandmarkFrame(Instant.now(), target == null ? build(poseId) : target); }
    private FaceDirection facingFor(String id) { return switch(id) {
        case "warrior_two" -> FaceDirection.LEFT;
        case "cat_cow", "downward_dog", "plank", "standing_fold" -> FaceDirection.DOWN;
        case "triangle" -> FaceDirection.UP;
        case "chair" -> FaceDirection.RIGHT;
        case "seated_fold", "head_to_knee" -> FaceDirection.RIGHT;
        case "bridge", "rest", "locust", "upward_salute" -> FaceDirection.UP;
        case "low_lunge" -> FaceDirection.LEFT;
        default -> FaceDirection.FRONT;
    }; }
    @Override public String transitionGuidance() { return transitionWaypoint == null
            ? "Move slowly while keeping each joint comfortable."
            : "Soften your knees, fold forward, place your hands down, then move one knee at a time."; }
    @Override public double spineBend() { return poseId.equals("cat_cow") ? Math.sin(System.nanoTime()/1_200_000_000.0)*.075 : 0; }
    private boolean crossesFloorBoundary(String from,String to) { return isFloor(from)!=isFloor(to); }
    private boolean isFloor(String id) { return switch(id){
        case "cat_cow","bridge","seated_fold","rest","easy_seat","seated_side_reach","seated_twist",
             "head_to_knee","downward_dog","plank","locust" -> true;
        default -> false;}; }
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
    // the deliberate dip that demonstrates the pause must sit below the gate
    private double confidence(){return frame%300>=290?.18:.94;}
    private void standing(EnumMap<LandmarkName, Landmark> p) {
        at(p, LandmarkName.NOSE,.50,.10); shoulders(p,.42,.21,.58,.21); arms(p,.40,.40,.39,.58,.60,.40,.61,.58);
        hips(p,.46,.48,.54,.48); legs(p,.46,.70,.45,.88,.54,.70,.55,.88); toes(p,.45,.96,.55,.96);
    }
    /**
     * Utkatasana, facing right: hips sink back BEHIND the heels while the chest
     * and arms travel forward over them. Drawn the other way round the figure
     * was leaning back over its own heels with the arms behind it, which is a
     * shape that falls over.
     */
    private void chair(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.60,.22); shoulders(p,.55,.33,.57,.34); arms(p,.68,.24,.80,.15,.70,.26,.82,.17);
        hips(p,.40,.56,.42,.57); legs(p,.55,.68,.55,.92,.57,.70,.57,.94); toes(p,.66,.95,.68,.955);
    }
    /**
     * Virabhadrasana I: a front knee bent over its ankle, and a long back leg.
     *
     * The coordinates are authored at the bone lengths constrain() enforces
     * (.23 thigh, .22 shin) rather than at whatever shape reads well on paper.
     * Authoring a deeper lunge than the bones allow does not deepen it: the
     * chain is rebuilt outward from the hip at fixed lengths, so a knee placed
     * too far away only changes the direction the thigh points and the leg
     * arrives straighter than it was drawn. That is how this pose came to sit
     * at 152 degrees while asking the body for 80 to 145 - it was telling a
     * practitioner to bend a knee that its own reference had straightened.
     *
     * Placing the ankle directly below the knee keeps the shin vertical, and
     * the thigh leaves the hip only 12 degrees above horizontal, which closes
     * the front knee to about 102 degrees: the thigh is nearly parallel to the
     * floor, which is the lunge the teaching illustration shows and the depth
     * this pose has been drawn short of all along. Going deeper necessarily
     * widens the stance - with the bones a fixed length, hips can only sink by
     * the feet moving apart.
     *
     * The back foot turns out, away from the midline, as the pose asks.
     */
    private void warriorOne(EnumMap<LandmarkName, Landmark> p) {
        // reaching up and slightly apart rather than together: drawn to the
        // midline both arms vanished into the head's own glow, leaving a lit
        // block above the shoulders instead of two arms
        arms(p,.40,.16,.36,.03,.60,.16,.64,.03); hips(p,.46,.50,.54,.50);
        legs(p,.2250,.5478,.2250,.7678,.7438,.6248,.9117,.7678); toes(p,.169,.825,.9597,.8318);
    }
    /**
     * Virabhadrasana II: the same front knee stacked over its ankle, a wider
     * stance, level arms, and the feet turned the way the pose actually asks -
     * front foot pointing along the gaze, back foot turned in across the mat.
     *
     * Authored at constrain()'s bone lengths, and to the same 102-degree front
     * knee as Warrior I, for the reasons given there. The back knee carries a
     * degree or two of bend so the straight leg reads as a leg rather than a
     * ruled line.
     *
     * The feet matter as much as the knee here: the front foot points along the
     * gaze, and the back foot turns in across the mat.
     */
    private void warriorTwo(EnumMap<LandmarkName, Landmark> p) {
        arms(p,.30,.24,.14,.24,.70,.24,.86,.24); hips(p,.46,.52,.54,.52);
        legs(p,.2550,.5678,.2550,.7878,.7138,.6448,.8817,.7878); toes(p,.195,.8406,.9297,.7238);
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
    /**
     * Tabletop, facing left: on hands and KNEES, which is the part this was
     * getting wrong.
     *
     * The legs were authored as one straight line from hip to toe, so
     * constrain() built a leg with a 178-degree knee: a stilt. The figure stood
     * on its hands and feet with both knees in the air, which is a bear crawl,
     * not the pose cat-cow is done from - and it is why the shape read as
     * floating however the feet were moved.
     *
     * The thigh now drops vertically to a knee resting on the floor, and the
     * shin runs backward along the floor to the ankle and toes, so all four
     * contacts a tabletop actually makes are on it. The arms hang straight from
     * the shoulders with the hands flat and pointing forward. Arm and thigh are
     * not the same length, so the back slopes a little toward the hips; that is
     * these proportions being honest rather than the pose being wrong.
     */
    private void tabletop(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.26,.70); shoulders(p,.3776,.66,.3976,.68); hips(p,.61,.72,.63,.74);
        arms(p,.3699,.81,.3699,.95,.4053,.845,.4053,.985);
        at(p,LandmarkName.LEFT_HAND,.30,.9423); at(p,LandmarkName.RIGHT_HAND,.3353,.9777);
        legs(p,.6059,.95,.83,.9459,.6341,.985,.8541,.9741);
        toes(p,.91,.9459,.9341,.9741);
    }
    private void lowLunge(EnumMap<LandmarkName, Landmark> p) {
        // Anjaneyasana, facing left: hips sunk to knee height, front shin
        // vertical over a flat foot, back knee and shin resting on the floor
        // with the foot pointed back, torso upright, arms reaching overhead.
        at(p,LandmarkName.NOSE,.44,.40); shoulders(p,.53,.50,.55,.50); arms(p,.55,.35,.57,.21,.57,.35,.59,.21);
        hips(p,.51,.74,.53,.74); legs(p,.30,.72,.30,.94,.66,.92,.875,.94); toes(p,.22,.95,.95,.955);
    }
    private void bridge(EnumMap<LandmarkName, Landmark> p) {
        // Setu Bandha, face up: head and shoulders rest on the floor, the
        // torso rises in a straight line to the hips and knees, feet stay
        // flat beneath the knees, arms press into the floor alongside.
        at(p,LandmarkName.NOSE,.17,.88); shoulders(p,.29,.90,.31,.90); arms(p,.44,.92,.58,.93,.46,.93,.60,.94);
        hips(p,.50,.785,.52,.785); legs(p,.70,.665,.70,.885,.72,.675,.72,.895); toes(p,.775,.92,.795,.93);
    }
    private void seatedFold(EnumMap<LandmarkName, Landmark> p) {
        // Paschimottanasana, facing right: sitting bones grounded, legs long
        // with feet flexed toward the ceiling, torso folded low over the
        // thighs, head released toward the shins, hands reaching to the feet.
        at(p,LandmarkName.NOSE,.70,.88); shoulders(p,.575,.815,.595,.825); arms(p,.72,.845,.855,.87,.74,.855,.875,.88);
        hips(p,.35,.895,.37,.905); legs(p,.58,.925,.80,.93,.59,.935,.81,.94); toes(p,.805,.855,.815,.865);
    }
    private void rest(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.17,.67); shoulders(p,.28,.70,.32,.73); arms(p,.41,.63,.54,.58,.43,.80,.56,.85);
        hips(p,.53,.70,.57,.73); legs(p,.70,.68,.84,.64,.72,.77,.86,.82); toes(p,.90,.63,.92,.82);
    }
    /**
     * Sukhasana, front view: sitting bones on the floor, thighs folded out to
     * the sides with the knees resting low, shins crossed in front.
     *
     * The legs must be authored folding roughly HORIZONTALLY, not hanging.
     * constrain() rebuilds each leg outward from the hip at standing bone
     * lengths (.23 thigh, .22 shin, .08 foot), so authoring the knee and ankle
     * below the hip pushes most of a leg beneath the pelvis. ground() then
     * rests the lowest point on the floor, which leaves the seat hovering a
     * thigh's length above it - the figure sits on a chair that is not there.
     * Folding the chain sideways keeps every bone length identical while
     * putting the hips, ankles and toes all within a few hundredths of the
     * floor, and leaves the knees just above it, which is where they belong.
     */
    private void easySeat(EnumMap<LandmarkName, Landmark> p, boolean reaching) {
        at(p,LandmarkName.NOSE,.50,.42); shoulders(p,.43,.54,.57,.54);
        hips(p,.46,.93,.54,.93); legs(p,.22,.91,.44,.93,.78,.91,.56,.93); toes(p,.52,.935,.48,.935);
        // Seating the hips drops the shoulders with them, so the arms are
        // re-authored against the seated shoulder height: the old coordinates
        // were written for a torso that started a third of the frame higher and
        // now read as pointing upward.
        if (reaching) arms(p,.38,.54,.44,.42,.66,.82,.79,.87);
        else arms(p,.34,.82,.21,.87,.66,.82,.79,.87);
    }
    private void headToKnee(EnumMap<LandmarkName, Landmark> p) {
        // one leg long, the other folded in, torso hinged over the long leg
        at(p,LandmarkName.NOSE,.62,.74); shoulders(p,.53,.70,.55,.72); arms(p,.68,.78,.80,.84,.70,.80,.82,.86);
        hips(p,.38,.86,.40,.88); legs(p,.60,.90,.80,.92,.48,.94,.56,.90); toes(p,.85,.86,.60,.94);
    }
    private void upwardSalute(EnumMap<LandmarkName, Landmark> p) {
        // standing tall, both arms sweeping overhead and nearly straight - the
        // elbow and wrist continue the shoulder's line so the reference honours
        // the straight-arm rule this pose carries
        at(p,LandmarkName.NOSE,.50,.10); shoulders(p,.44,.21,.56,.21); arms(p,.42,.07,.40,-.08,.58,.07,.60,-.08);
        hips(p,.46,.48,.54,.48); legs(p,.46,.70,.45,.88,.54,.70,.55,.88); toes(p,.45,.96,.55,.96);
    }
    private void standingFold(EnumMap<LandmarkName, Landmark> p) {
        // hinged from the hips with the head and arms hanging heavy
        at(p,LandmarkName.NOSE,.48,.62); shoulders(p,.46,.54,.54,.55); arms(p,.45,.68,.44,.82,.55,.68,.56,.82);
        hips(p,.46,.44,.54,.44); legs(p,.46,.68,.45,.88,.54,.68,.55,.88); toes(p,.44,.95,.56,.95);
    }
    private void downwardDog(EnumMap<LandmarkName, Landmark> p) {
        // inverted V: hands and feet down, hips lifted high
        at(p,LandmarkName.NOSE,.26,.62); shoulders(p,.32,.56,.34,.58); arms(p,.24,.72,.17,.88,.26,.74,.19,.90);
        at(p,LandmarkName.LEFT_HAND,.13,.94); at(p,LandmarkName.RIGHT_HAND,.15,.95);
        hips(p,.60,.34,.62,.36); legs(p,.70,.60,.78,.86,.72,.62,.80,.88); toes(p,.83,.94,.85,.95);
    }
    /**
     * Goddess: a wide stance with the thighs sunk to parallel with the floor.
     *
     * Authored at constrain()'s bone lengths, like the warriors, so the squat
     * is as deep as it is drawn: the thigh leaves the hip almost level and the
     * shin drops vertically, which is the shape the pose is named for. Drawn
     * shallower the hips sat barely below standing height.
     */
    private void goddess(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.50,.26); shoulders(p,.42,.38,.58,.38); arms(p,.30,.44,.28,.28,.70,.44,.72,.28);
        hips(p,.43,.64,.57,.64); legs(p,.2201,.6480,.2201,.8680,.7799,.6480,.7799,.8680); toes(p,.1721,.9320,.8279,.9320);
    }
    private void plank(EnumMap<LandmarkName, Landmark> p) {
        // one long line from heels to crown, supported on the hands
        at(p,LandmarkName.NOSE,.20,.66); shoulders(p,.30,.70,.32,.72); arms(p,.29,.80,.28,.90,.31,.82,.30,.92);
        at(p,LandmarkName.LEFT_HAND,.27,.95); at(p,LandmarkName.RIGHT_HAND,.29,.95);
        hips(p,.58,.72,.60,.74); legs(p,.74,.78,.88,.86,.76,.80,.90,.88); toes(p,.92,.94,.94,.95);
    }
    private void locust(EnumMap<LandmarkName, Landmark> p) {
        // face down, chest and legs lifted a comfortable amount
        at(p,LandmarkName.NOSE,.20,.78); shoulders(p,.30,.84,.32,.85); arms(p,.44,.90,.58,.92,.46,.91,.60,.93);
        hips(p,.60,.90,.62,.91); legs(p,.76,.88,.90,.82,.78,.89,.92,.84); toes(p,.94,.78,.96,.80);
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
