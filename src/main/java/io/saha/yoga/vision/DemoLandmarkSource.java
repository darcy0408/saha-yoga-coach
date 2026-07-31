package io.saha.yoga.vision;

import io.saha.yoga.domain.*;
import java.time.Instant;
import java.util.EnumMap;

public final class DemoLandmarkSource implements LandmarkSource {
    private long frame;
    private String poseId = "mountain";
    @Override public void selectPose(String poseId) { this.poseId = poseId; }
    @Override public LandmarkFrame nextFrame() {
        var points = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        standing(points);
        switch (poseId) {
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
        frame++;
        return new LandmarkFrame(Instant.now(), points);
    }
    private void standing(EnumMap<LandmarkName, Landmark> p) {
        at(p, LandmarkName.NOSE,.50,.10); shoulders(p,.42,.21,.58,.21); arms(p,.40,.40,.39,.58,.60,.40,.61,.58);
        hips(p,.46,.48,.54,.48); legs(p,.46,.70,.45,.92,.54,.70,.55,.92);
    }
    private void chair(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.50,.12); shoulders(p,.42,.23,.58,.23); arms(p,.38,.12,.34,.05,.62,.12,.66,.05);
        hips(p,.42,.52,.52,.52); legs(p,.55,.68,.48,.90,.63,.68,.57,.90);
    }
    private void warriorOne(EnumMap<LandmarkName, Landmark> p) {
        arms(p,.40,.12,.44,.04,.60,.12,.56,.04); hips(p,.45,.48,.55,.48); legs(p,.36,.68,.27,.91,.64,.67,.79,.90);
    }
    private void warriorTwo(EnumMap<LandmarkName, Landmark> p) {
        arms(p,.29,.22,.16,.22,.71,.22,.84,.22); hips(p,.45,.48,.55,.48); legs(p,.35,.67,.23,.90,.65,.67,.82,.90);
    }
    private void triangle(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.43,.18); shoulders(p,.36,.28,.50,.20); arms(p,.41,.43,.31,.59,.52,.10,.55,.03);
        hips(p,.45,.49,.55,.46); legs(p,.35,.68,.22,.90,.66,.67,.82,.90);
    }
    private void tree(EnumMap<LandmarkName, Landmark> p) {
        arms(p,.40,.12,.47,.04,.60,.12,.53,.04); hips(p,.46,.48,.54,.48);
        legs(p,.46,.70,.46,.92,.64,.66,.48,.70);
    }
    private void tabletop(EnumMap<LandmarkName, Landmark> p, boolean extended) {
        at(p,LandmarkName.NOSE,.24,.43); shoulders(p,.31,.48,.36,.53); hips(p,.57,.50,.62,.55);
        arms(p,.35,.66,.34,.84, extended?.20:.39,extended?.39:.83,.73,.82);
        legs(p,.56,.70,.55,.87,.69,.68,extended?.88:.72,extended?.48:.86);
    }
    private void lowLunge(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.48,.17); shoulders(p,.42,.28,.56,.28); arms(p,.38,.47,.32,.65,.60,.47,.66,.65);
        hips(p,.44,.52,.55,.52); legs(p,.35,.69,.25,.90,.68,.72,.83,.88);
    }
    private void bridge(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.18,.68); shoulders(p,.27,.70,.31,.74); arms(p,.38,.77,.49,.82,.39,.78,.50,.83);
        hips(p,.52,.55,.57,.59); legs(p,.66,.68,.75,.84,.70,.70,.80,.84);
    }
    private void seatedFold(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.38,.46); shoulders(p,.40,.50,.48,.54); arms(p,.56,.62,.68,.72,.57,.64,.70,.74);
        hips(p,.48,.68,.54,.69); legs(p,.66,.74,.82,.78,.68,.78,.84,.82);
    }
    private void rest(EnumMap<LandmarkName, Landmark> p) {
        at(p,LandmarkName.NOSE,.18,.68); shoulders(p,.29,.70,.35,.70); arms(p,.43,.62,.53,.57,.43,.78,.53,.83);
        hips(p,.55,.70,.60,.70); legs(p,.70,.70,.84,.68,.72,.75,.86,.78);
    }
    private void shoulders(EnumMap<LandmarkName, Landmark> p,double lx,double ly,double rx,double ry){at(p,LandmarkName.LEFT_SHOULDER,lx,ly);at(p,LandmarkName.RIGHT_SHOULDER,rx,ry);}
    private void hips(EnumMap<LandmarkName, Landmark> p,double lx,double ly,double rx,double ry){at(p,LandmarkName.LEFT_HIP,lx,ly);at(p,LandmarkName.RIGHT_HIP,rx,ry);}
    private void arms(EnumMap<LandmarkName, Landmark> p,double lex,double ley,double lwx,double lwy,double rex,double rey,double rwx,double rwy){at(p,LandmarkName.LEFT_ELBOW,lex,ley);at(p,LandmarkName.LEFT_WRIST,lwx,lwy);at(p,LandmarkName.RIGHT_ELBOW,rex,rey);at(p,LandmarkName.RIGHT_WRIST,rwx,rwy);}
    private void legs(EnumMap<LandmarkName, Landmark> p,double lkx,double lky,double lax,double lay,double rkx,double rky,double rax,double ray){at(p,LandmarkName.LEFT_KNEE,lkx,lky);at(p,LandmarkName.LEFT_ANKLE,lax,lay);at(p,LandmarkName.RIGHT_KNEE,rkx,rky);at(p,LandmarkName.RIGHT_ANKLE,rax,ray);}
    private void at(EnumMap<LandmarkName, Landmark> p, LandmarkName name,double x,double y){ put(p,name,x,y); }
    private void put(EnumMap<LandmarkName, Landmark> map, LandmarkName name, double x, double y) {
        map.put(name, new Landmark(x, y, frame % 30 == 29 ? .55 : .94));
    }
    @Override public String description() { return "Demo landmarks · " + poseId.replace('_', ' '); }
}
