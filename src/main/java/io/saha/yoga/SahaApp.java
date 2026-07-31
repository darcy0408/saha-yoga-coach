package io.saha.yoga;

import io.saha.yoga.analysis.*;
import io.saha.yoga.domain.*;
import io.saha.yoga.personalization.PersonalizationEngine;
import io.saha.yoga.routine.*;
import io.saha.yoga.storage.*;
import io.saha.yoga.vision.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public final class SahaApp extends Application {
    private final PoseCatalog catalog = new PoseCatalog();
    private final RoutineGenerator generator = new RoutineGenerator(catalog);
    private final PoseAnalyzer analyzer = new PoseAnalyzer();
    private final LandmarkSource landmarks = new DemoLandmarkSource();
    private final SessionStore store = new JsonSessionStore(Path.of(System.getProperty("user.home"), ".saha", "sessions.json"));
    private Stage stage;
    private Routine routine;
    private int itemIndex, remaining;
    private boolean paused;
    private Timeline clock;
    private Label poseLabel, phaseLabel, statusLabel, suggestionLabel, optionalLabel, confidenceLabel, timerLabel;
    private Pane bodyView;

    @Override public void start(Stage primaryStage) {
        stage = primaryStage; stage.setTitle("Saha · personal yoga coach");
        stage.setMinWidth(900); stage.setMinHeight(650);
        showWelcome(); stage.show();
    }

    private void setPage(Region content) {
        var scene = new Scene(content, 1080, 720);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/saha/yoga/saha.css")).toExternalForm());
        stage.setScene(scene);
    }

    private ScrollPane scrollable(Region content) {
        var scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("page-scroll");
        return scroll;
    }

    private void showWelcome() {
        var title = new Label("Move with awareness."); title.getStyleClass().add("hero");
        var intro = new Label("Saha guides a gentle practice using anonymous body landmarks. Camera images stay on this device and are not saved."); intro.setWrapText(true); intro.setMaxWidth(1050); intro.setMinHeight(Region.USE_PREF_SIZE); intro.getStyleClass().add("lead");
        var experience = new ComboBox<String>(); experience.getItems().addAll("New to yoga", "Some experience", "Regular practice"); experience.getSelectionModel().selectFirst();
        var goal = new ComboBox<String>(); goal.getItems().addAll("Gentle movement", "Flexibility", "Balance", "Strength", "Recovery"); goal.getSelectionModel().selectFirst();
        var mobility = new TextField(); mobility.setPromptText("Optional movement limits or areas to avoid");
        var intensity = new Slider(1, 3, 1); intensity.setShowTickLabels(true); intensity.setMajorTickUnit(1); intensity.setSnapToTicks(true);
        var consent = new CheckBox("I understand Saha is educational fitness software, not medical care."); consent.setWrapText(true); consent.setMinHeight(Region.USE_PREF_SIZE);
        var safety = new Label("Stop immediately for pain, dizziness, numbness, weakness, or unusual discomfort. For pregnancy, recent surgery, chronic pain, or significant mobility limits, seek appropriate professional guidance."); safety.setWrapText(true); safety.setMinHeight(Region.USE_PREF_SIZE); safety.getStyleClass().add("notice");
        var start = new Button("Continue to camera setup"); start.getStyleClass().add("primary"); start.disableProperty().bind(consent.selectedProperty().not()); start.setOnAction(e -> showCalibration());
        var form = new VBox(14, field("Experience", experience), field("Focus", goal), field("Anything we should avoid?", mobility), field("Preferred intensity · gentle to active", intensity), consent, safety, start);
        form.getStyleClass().add("card"); form.setMaxWidth(700);
        var page = new VBox(24, new Label("SAHA  /  PRIVATE BY DEFAULT"), title, intro, form); page.setAlignment(Pos.CENTER_LEFT); page.setPadding(new Insets(55, 100, 55, 100)); page.getStyleClass().add("page");
        setPage(scrollable(page));
    }

    private VBox field(String name, Control control) { var label = new Label(name); label.getStyleClass().add("field-label"); control.setMaxWidth(Double.MAX_VALUE); return new VBox(6, label, control); }

    private void showCalibration() {
        var title = new Label("Set up your space"); title.getStyleClass().add("title");
        var guide = new VBox(12, check("Place the camera around hip height."), check("Step back until your whole body fits."), check("Face a light source; avoid a bright window behind you."), check("Clear enough floor space to step in every direction.")); guide.getStyleClass().add("card");
        var preview = createBodyView(); bodyView = preview; preview.setPrefSize(480, 390); preview.getStyleClass().add("camera");
        var badge = new Label("DEMO MODE · no camera required"); badge.getStyleClass().add("badge");
        var note = new Label("Camera integration is safely unavailable until a compatible ONNX pose model is installed. The demonstration uses prerecorded-style synthetic landmarks and exercises the same analysis pipeline."); note.setWrapText(true);
        var begin = new Button("Start Steady Start"); begin.getStyleClass().add("primary"); begin.setOnAction(e -> beginRoutine());
        var left = new VBox(18, title, guide, badge, note, begin); left.setMaxWidth(470);
        var page = new BorderPane(preview, null, null, null, left); page.setPadding(new Insets(50)); BorderPane.setMargin(left, new Insets(0, 35, 0, 0)); page.getStyleClass().add("page");
        drawFrame(landmarks.nextFrame()); setPage(page);
    }

    private HBox check(String value) { var dot = new Label("✓"); dot.getStyleClass().add("check"); var text = new Label(value); text.setWrapText(true); return new HBox(10, dot, text); }

    private void beginRoutine() {
        try { var recommendation = new PersonalizationEngine().recommend(store.load()); routine = generator.beginner(recommendation.durationAdjustments(), recommendation.explanations()); }
        catch (IOException e) { routine = generator.beginner(Map.of(), List.of("Session history could not be read; using the gentle baseline.")); }
        itemIndex = 0; remaining = routine.items().getFirst().durationSeconds(); showCoach();
    }

    private void showCoach() {
        poseLabel = new Label(); poseLabel.getStyleClass().add("hero-small"); phaseLabel = new Label(); phaseLabel.getStyleClass().add("badge");
        statusLabel = new Label(); statusLabel.getStyleClass().add("status"); suggestionLabel = wrapLabel(); optionalLabel = wrapLabel(); confidenceLabel = new Label(); timerLabel = new Label(); timerLabel.getStyleClass().add("timer");
        bodyView = createBodyView(); bodyView.setPrefSize(560, 500); bodyView.getStyleClass().add("camera");
        var stop = actionButton("Stop now"); stop.getStyleClass().add("danger"); stop.setOnAction(e -> finish(false));
        var pause = actionButton("Pause"); pause.setOnAction(e -> { paused = !paused; pause.setText(paused ? "Resume" : "Pause"); });
        var repeat = actionButton("Repeat cue"); repeat.setOnAction(e -> suggestionLabel.requestFocus());
        var easier = actionButton("Easier option"); easier.setOnAction(e -> optionalLabel.setText("Optional adjustment: " + current().pose().modifications().getFirst()));
        var next = actionButton("Next pose"); next.getStyleClass().add("next"); next.setOnAction(e -> advance(true));
        var controls = new GridPane(); controls.setHgap(10); controls.setVgap(10);
        var leftColumn = new ColumnConstraints(); leftColumn.setPercentWidth(50);
        var rightColumn = new ColumnConstraints(); rightColumn.setPercentWidth(50);
        controls.getColumnConstraints().addAll(leftColumn, rightColumn);
        controls.add(pause, 0, 0); controls.add(repeat, 1, 0);
        controls.add(easier, 0, 1); controls.add(next, 1, 1);
        controls.add(stop, 0, 2, 2, 1);
        var feedback = new VBox(10, phaseLabel, poseLabel, timerLabel, statusLabel, suggestionLabel, optionalLabel, confidenceLabel, new Separator(), new Label("Why this routine changed"), new Label(String.join(" ", routine.explanations())), controls); feedback.getStyleClass().add("card"); feedback.setMaxWidth(440);
        var page = new BorderPane(bodyView, null, feedback, null, null); page.setPadding(new Insets(35)); BorderPane.setMargin(feedback, new Insets(0, 0, 0, 25)); page.getStyleClass().add("page");
        setPage(page); updatePose();
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick())); clock.setCycleCount(Timeline.INDEFINITE); clock.play();
    }

    private Label wrapLabel() { var label = new Label(); label.setWrapText(true); return label; }
    private Button actionButton(String text) { var button = new Button(text); button.setMaxWidth(Double.MAX_VALUE); return button; }
    private RoutineItem current() { return routine.items().get(itemIndex); }
    private void tick() {
        var frame = landmarks.nextFrame(); drawFrame(frame); var result = analyzer.analyze(current().pose(), frame);
        boolean reliable = result instanceof AnalysisResult.Reliable;
        switch (result) {
            case AnalysisResult.Reliable r -> {
                statusLabel.setText("Status: " + r.status());
                suggestionLabel.setText("Primary suggestion: " + (r.suggestions().isEmpty() ? "Keep breathing comfortably." : r.suggestions().getFirst()));
                optionalLabel.setText("Optional adjustment: " + current().pose().modifications().getFirst());
                confidenceLabel.setText("Confidence: " + level(r.confidence()));
            }
            case AnalysisResult.Unreliable u -> {
                statusLabel.setText("Status: Camera view needs attention"); suggestionLabel.setText("Primary suggestion: " + u.guidance());
                optionalLabel.setText("Corrections are paused until the view improves."); confidenceLabel.setText("Confidence: Low");
            }
        }
        if (!paused && reliable && --remaining <= 0) advance(false);
        timerLabel.setText(format(remaining) + (paused || !reliable ? " · paused" : ""));
    }
    private String level(double value) { return value >= .85 ? "High" : value >= .70 ? "Medium" : "Low"; }
    private String format(int seconds) { return "%d:%02d".formatted(seconds / 60, seconds % 60); }
    private void updatePose() { var item = current(); poseLabel.setText("Current pose: " + item.pose().displayName()); phaseLabel.setText(item.phase().toUpperCase()); timerLabel.setText(format(remaining)); }
    private void advance(boolean skipped) {
        saveMetric(skipped); if (++itemIndex >= routine.items().size()) { finish(true); return; }
        remaining = current().durationSeconds(); updatePose();
    }
    private void saveMetric(boolean skipped) {
        try { store.append(new SessionMetric(current().pose().id(), Instant.now(), current().durationSeconds() - remaining, skipped ? 0 : .82, 1, skipped, .91, !skipped)); } catch (IOException ignored) { }
    }
    private void finish(boolean completed) { if (clock != null) clock.stop(); showProgress(completed); }

    private void showProgress(boolean completed) {
        List<SessionMetric> history; try { history = store.load(); } catch (IOException e) { history = List.of(); }
        long completedPoses = history.stream().filter(m -> !m.skipped()).count();
        double stability = history.stream().mapToDouble(SessionMetric::stability).average().orElse(0);
        var title = new Label(completed ? "Practice complete" : "Practice stopped"); title.getStyleClass().add("hero");
        var stats = new HBox(16, stat("POSES COMPLETED", Long.toString(completedPoses)), stat("AVERAGE STABILITY", Math.round(stability * 100) + "%"), stat("LOCAL RECORDS", Integer.toString(history.size())));
        var privacy = new Label("Only derived session metrics are stored locally. No images or landmark coordinates are saved."); privacy.setWrapText(true);
        var delete = new Button("Delete all local history"); delete.setOnAction(e -> { try { store.deleteAll(); showProgress(completed); } catch (IOException ex) { privacy.setText("Could not delete local history: " + ex.getMessage()); } });
        var again = new Button("Plan another practice"); again.getStyleClass().add("primary"); again.setOnAction(e -> showWelcome());
        var page = new VBox(24, title, new Label("Progress, without body scoring."), stats, privacy, new HBox(10, again, delete)); page.setAlignment(Pos.CENTER_LEFT); page.setPadding(new Insets(80)); page.getStyleClass().add("page"); setPage(page);
    }
    private VBox stat(String label, String value) { var number = new Label(value); number.getStyleClass().add("stat-number"); var box = new VBox(7, new Label(label), number); box.getStyleClass().add("card"); HBox.setHgrow(box, Priority.ALWAYS); return box; }

    private Pane createBodyView() { var pane = new Pane(); pane.setMinSize(420, 380); return pane; }
    private void drawFrame(LandmarkFrame frame) {
        if (bodyView == null) return; bodyView.getChildren().clear(); double w = Math.max(420, bodyView.getWidth()), h = Math.max(380, bodyView.getHeight());
        var links = List.of(new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.RIGHT_SHOULDER}, new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.LEFT_HIP}, new LandmarkName[]{LandmarkName.RIGHT_SHOULDER,LandmarkName.RIGHT_HIP}, new LandmarkName[]{LandmarkName.LEFT_HIP,LandmarkName.RIGHT_HIP}, new LandmarkName[]{LandmarkName.LEFT_HIP,LandmarkName.LEFT_KNEE}, new LandmarkName[]{LandmarkName.LEFT_KNEE,LandmarkName.LEFT_ANKLE}, new LandmarkName[]{LandmarkName.RIGHT_HIP,LandmarkName.RIGHT_KNEE}, new LandmarkName[]{LandmarkName.RIGHT_KNEE,LandmarkName.RIGHT_ANKLE}, new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.LEFT_ELBOW}, new LandmarkName[]{LandmarkName.LEFT_ELBOW,LandmarkName.LEFT_WRIST}, new LandmarkName[]{LandmarkName.RIGHT_SHOULDER,LandmarkName.RIGHT_ELBOW}, new LandmarkName[]{LandmarkName.RIGHT_ELBOW,LandmarkName.RIGHT_WRIST});
        for (var link : links) { var a=frame.landmarks().get(link[0]); var b=frame.landmarks().get(link[1]); var line=new Line(a.x()*w,a.y()*h,b.x()*w,b.y()*h); line.setStroke(Color.web("#8dd7c6")); line.setStrokeWidth(5); bodyView.getChildren().add(line); }
        frame.landmarks().values().forEach(p -> { var circle=new Circle(p.x()*w,p.y()*h,6,Color.web("#f4c77a")); bodyView.getChildren().add(circle); });
        var nose = frame.landmarks().get(LandmarkName.NOSE);
        var leftShoulder = frame.landmarks().get(LandmarkName.LEFT_SHOULDER);
        var rightShoulder = frame.landmarks().get(LandmarkName.RIGHT_SHOULDER);
        var head = new Circle(nose.x()*w, nose.y()*h, Math.max(18, w*.032), Color.TRANSPARENT);
        head.setStroke(Color.web("#8dd7c6")); head.setStrokeWidth(5);
        var neck = new Line(nose.x()*w, (nose.y()*h)+Math.max(18, w*.032),
                ((leftShoulder.x()+rightShoulder.x())/2)*w, ((leftShoulder.y()+rightShoulder.y())/2)*h);
        neck.setStroke(Color.web("#8dd7c6")); neck.setStrokeWidth(5);
        bodyView.getChildren().addAll(neck, head);
        var label = new Text(18, 28, landmarks.description()); label.setFill(Color.web("#b7c8c5")); bodyView.getChildren().add(label);
    }

    @Override public void stop() { if (clock != null) clock.stop(); landmarks.close(); }
    public static void main(String[] args) { launch(args); }
}
