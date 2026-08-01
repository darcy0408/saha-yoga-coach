package io.saha.yoga.illustration;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;

import static io.saha.yoga.illustration.BodyAnchor.*;

public final class TeachingPoseDraftView extends Pane {
    private static final Color FRONT = Color.web("#173532");
    private static final Color REAR = Color.web("#75958e");
    private static final Color JOINT = Color.web("#c88b32");
    private final TeachingPoseDraft draft;

    public TeachingPoseDraftView(TeachingPoseDraft draft) {
        this.draft = draft;
        setMinSize(280, 220);
        setPrefSize(360, 300);
        widthProperty().addListener((ignored, oldValue, newValue) -> redraw());
        heightProperty().addListener((ignored, oldValue, newValue) -> redraw());
    }

    private void redraw() {
        getChildren().clear();
        double width = Math.max(280, getWidth());
        double height = Math.max(220, getHeight());
        double floor = draft.floorY() * height;

        var ground = line(width*.06, floor, width*.94, floor, 3, Color.web("#8c6b32"));
        getChildren().add(ground);
        var floorText = new Text(width*.07, floor-7, "support surface"); floorText.setFill(Color.web("#725b25"));
        getChildren().add(floorText);

        leg(REAR_KNEE, REAR_ANKLE, REAR_HEEL, REAR_TOE, width, height, REAR);
        limb(HIP, REAR_KNEE, width, height, 11, REAR);
        arm(REAR_ELBOW, REAR_HAND, width, height, REAR);

        limb(SHOULDER, HIP, width, height, 29, Color.web("#b7d1ca"));
        limb(SHOULDER, HIP, width, height, 7, FRONT);
        var hip = point(HIP,width,height);
        var pelvis = new Ellipse(hip[0],hip[1],width*.035,height*.022); pelvis.setFill(Color.web("#b7d1ca")); pelvis.setStroke(FRONT); pelvis.setStrokeWidth(3);
        getChildren().add(pelvis);

        leg(FRONT_KNEE, FRONT_ANKLE, FRONT_HEEL, FRONT_TOE, width, height, FRONT);
        limb(HIP, FRONT_KNEE, width, height, 12, FRONT);
        arm(FRONT_ELBOW, FRONT_HAND, width, height, FRONT);

        limb(NECK, SHOULDER, width, height, 8, FRONT);
        var head = point(HEAD,width,height);
        var neck = point(NECK,width,height);
        limb(head[0], head[1]+height*.055, neck[0], neck[1], 7, FRONT);
        var headShape = new Ellipse(head[0],head[1],width*.038,height*.055); headShape.setFill(Color.web("#f6ead0")); headShape.setStroke(FRONT); headShape.setStrokeWidth(5);
        getChildren().add(headShape);
        var eye = new Circle(head[0]+width*.015,head[1]-height*.012,2.5,JOINT);
        var nose = line(head[0]+width*.027,head[1],head[0]+width*.045,head[1]+height*.006,2.5,JOINT);
        getChildren().addAll(eye,nose);

        var label = new Text(width*.68,height*.08,"DRAFT · REVIEW ONLY"); label.setFill(Color.web("#9f4646"));
        getChildren().add(label);
    }

    private void arm(BodyAnchor elbow, BodyAnchor hand, double width, double height, Color color) {
        limb(SHOULDER,elbow,width,height,9,color); limb(elbow,hand,width,height,8,color); joint(elbow,width,height); joint(hand,width,height);
    }

    private void leg(BodyAnchor knee, BodyAnchor ankle, BodyAnchor heel, BodyAnchor toe, double width, double height, Color color) {
        limb(knee,ankle,width,height,12,color); limb(ankle,heel,width,height,8,color); limb(heel,toe,width,height,11,color);
        joint(knee,width,height); joint(ankle,width,height);
    }

    private void limb(BodyAnchor from, BodyAnchor to, double width, double height, double stroke, Color color) {
        var a=point(from,width,height); var b=point(to,width,height); getChildren().add(line(a[0],a[1],b[0],b[1],stroke,color));
    }

    private void limb(double x1,double y1,double x2,double y2,double stroke,Color color) { getChildren().add(line(x1,y1,x2,y2,stroke,color)); }
    private Line line(double x1,double y1,double x2,double y2,double stroke,Color color) {
        var line=new Line(x1,y1,x2,y2); line.setStroke(color); line.setStrokeWidth(stroke); line.setStrokeLineCap(StrokeLineCap.ROUND); return line;
    }
    private double[] point(BodyAnchor anchor,double width,double height) { var p=draft.point(anchor); return new double[]{p.x()*width,p.y()*height}; }
    private void joint(BodyAnchor anchor,double width,double height) { var p=point(anchor,width,height); getChildren().add(new Circle(p[0],p[1],4,JOINT)); }
}
