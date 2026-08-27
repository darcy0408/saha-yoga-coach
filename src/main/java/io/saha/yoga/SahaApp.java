package io.saha.yoga;

import io.saha.yoga.analysis.*;
import io.saha.yoga.domain.*;
import io.saha.yoga.illustration.*;
import io.saha.yoga.personalization.PersonalizationEngine;
import io.saha.yoga.routine.*;
import io.saha.yoga.speech.SpokenCoach;
import io.saha.yoga.speech.SystemVoice;
import io.saha.yoga.speech.Voice;
import io.saha.yoga.storage.*;
import io.saha.yoga.vision.*;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.QuadCurve;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class SahaApp extends Application {
    private final PoseCatalog catalog = new PoseCatalog();
    private final RoutineGenerator generator = new RoutineGenerator(catalog);
    private final PoseAnalyzer analyzer = new PoseAnalyzer();
    private final SpokenCoach spoken = new SpokenCoach();
    private final io.saha.yoga.sound.Chime chime = new io.saha.yoga.sound.Chime();
    /** So the chime marks the moment a pose comes right, not every frame it stays right. */
    private boolean wasAligned;
    private Voice voice = Voice.silent();
    private boolean speechWanted = true;
    /** Show the camera as a mirror, which is what people expect of a self view. */
    private boolean mirrorPreview = true;
    /**
     * Draw only the figure, never the picture.
     *
     * The room is not needed to coach a pose, and showing it puts the
     * television, the laundry and anyone walking past on screen — and into any
     * recording made of the screen. The landmarks are the person; everything
     * else was only ever incidental.
     */
    private boolean figureOnly = true;
    /**
     * Swap red and blue in the preview.
     *
     * Not a bug being preserved: the colours reaching the view are correct, and
     * this deliberately trades them for the cooler cast the entrant preferred
     * while practising. The frames handed to the model are untouched, so
     * nothing about the estimate changes with this setting.
     */
    private boolean stylizedColour = true;
    private byte[] tintScratch;
    private final DemoLandmarkSource demoSource = new DemoLandmarkSource();
    /** Swapped to the camera source once a verified model is driving real landmarks. */
    private LandmarkSource landmarks = demoSource;
    private CameraLandmarkSource cameraSource;
    private final PoseIllustrationRegistry illustrations = new PoseIllustrationRegistry();
    private final TeachingAssetCatalog teachingAssets = new TeachingAssetCatalog();
    private final PoseIconCatalog poseIcons = new PoseIconCatalog();
    private final TeachingPoseDraftCatalog teachingDrafts = new TeachingPoseDraftCatalog();
    private final SessionStore store = new JsonSessionStore(Path.of(System.getProperty("user.home"), ".saha", "sessions.json"));
    private Stage stage;
    private Routine routine;
    private int itemIndex, remaining;
    private int preferredIntensity = 2;
    private int clockTicks;
    private boolean paused;
    private Timeline clock;
    private Label poseLabel, phaseLabel, statusLabel, suggestionLabel, optionalLabel, confidenceLabel, timerLabel, measurementLabel;
    /**
     * Off by default and reachable only during a camera practice. This is the
     * instrument for the live checks the crop and capture changes are waiting
     * on: raw model scores per joint, the patch the model was shown, and the
     * pace the pipeline sustains. It shows numbers the coach otherwise rounds
     * into words, so it stays out of the way unless someone asks.
     */
    private boolean showDiagnostics;
    private Label diagnosticsLabel;
    private Pane bodyView;
    private Pane landmarkOverlay;
    private VBox teachingView;
    private HBox practicePath;
    private ScrollPane practicePathScroll;
    private CameraCapture cameraCapture;
    private ImageView cameraPreview;
    private Label cameraStatus;
    private Button cameraButton;
    private PauseTransition cameraOpenTimeout;
    private final AtomicReference<CameraFrame> pendingCameraFrame = new AtomicReference<>();
    private final AtomicBoolean cameraRenderPending = new AtomicBoolean();
    private volatile boolean livePreviewActive;
    /** How many raw device indices the switch button cycles through. */
    private static final int CAMERA_DEVICES = 4;
    /** Raw OS device index. Which physical camera it means is the OS's choice, so it is selectable. */
    private int cameraDevice;
    private Button switchCameraButton;
    /** Why estimation is off, when the picture is running without it. Empty while it is working. */
    private String modelFallbackReason = "";

    @Override public void start(Stage primaryStage) {
        stage = primaryStage; stage.setTitle("Saha · personal yoga coach");
        stage.setMinWidth(900); stage.setMinHeight(650);
        if (getParameters().getRaw().contains("--pose-gallery")) showPoseGallery();
        else showWelcome();
        stage.show();
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
        var intensity = new Slider(1, 5, 2); intensity.setShowTickLabels(true); intensity.setShowTickMarks(true); intensity.setMajorTickUnit(1); intensity.setMinorTickCount(0); intensity.setBlockIncrement(1); intensity.setSnapToTicks(true);
        var consent = new CheckBox("I understand Saha is educational fitness software, not medical care."); consent.setWrapText(true); consent.setMinHeight(Region.USE_PREF_SIZE);
        var safety = new Label("Stop immediately for pain, dizziness, numbness, weakness, or unusual discomfort. For pregnancy, recent surgery, chronic pain, or significant mobility limits, seek appropriate professional guidance."); safety.setWrapText(true); safety.setMinHeight(Region.USE_PREF_SIZE); safety.getStyleClass().add("notice");
        var start = new Button("Continue to camera setup"); start.getStyleClass().add("primary"); start.disableProperty().bind(consent.selectedProperty().not()); start.setOnAction(e -> { preferredIntensity = (int) Math.round(intensity.getValue()); showCalibration(); });
        var form = new VBox(14, field("Experience", experience), field("Focus", goal), field("Anything we should avoid?", mobility), field("Preferred intensity · 1 gentle to 5 active", intensity), consent, safety, start);
        form.getStyleClass().add("card"); form.setMaxWidth(700);
        var page = new VBox(24, new Label("SAHA  /  PRIVATE BY DEFAULT"), title, intro, form); page.setAlignment(Pos.CENTER_LEFT); page.setPadding(new Insets(55, 100, 55, 100)); page.getStyleClass().add("page");
        setPage(scrollable(page));
    }

    private VBox field(String name, Control control) { var label = new Label(name); label.getStyleClass().add("field-label"); control.setMaxWidth(Double.MAX_VALUE); return new VBox(6, label, control); }

    private void showCalibration() {
        stopCameraPreview();
        var title = new Label("Set up your space"); title.getStyleClass().add("title");
        // "until your whole body fits" is not far enough: several poses reach
        // overhead, and a body that exactly fills the frame loses its arms the
        // moment they go up - which is the pose the coach then cannot measure
        var guide = new VBox(12, check("Place the camera around hip height."), check("Reach both arms overhead and step back until your hands still fit in the frame."), check("Face a light source; avoid a bright window behind you."), check("Clear enough floor space to step in every direction.")); guide.getStyleClass().add("card");
        var preview = createBodyView(); bodyView = preview; preview.setPrefSize(560, 460);
        cameraPreview = new ImageView(); cameraPreview.setPreserveRatio(true); cameraPreview.setFitWidth(560); cameraPreview.setFitHeight(460); cameraPreview.setVisible(false);
        cameraPreview.setScaleX(mirrorPreview ? -1 : 1);
        landmarkOverlay = createOverlay();
        var previewStack = new StackPane(preview, cameraPreview, landmarkOverlay); previewStack.setPrefSize(560, 460); previewStack.setMaxSize(620, 520); previewStack.getStyleClass().add("camera");
        var badge = new Label("DEMO COACHING · local camera preview optional"); badge.getStyleClass().add("badge");
        cameraStatus = new Label("No camera is opened unless you choose the preview below."); cameraStatus.setWrapText(true); cameraStatus.setMinHeight(Region.USE_PREF_SIZE);
        var note = new Label("Camera preview stays on this device and is never recorded. Until a verified ONNX pose model is installed, coaching continues with synthetic demonstration landmarks and does not claim to analyze the preview."); note.setWrapText(true);
        cameraButton = new Button("Try local camera preview"); cameraButton.setOnAction(e -> startCameraPreview(cameraDevice));
        // Which physical camera index 0 means is the operating system's choice,
        // not ours. A machine with a virtual camera - streaming software, a
        // conferencing tool - can hand back a placeholder image with nobody in
        // it, and the coach then looks broken while behaving perfectly. This
        // cycles rather than listing: no device is probed to build a list,
        // because opening a camera to discover it exists is still opening it.
        switchCameraButton = new Button(switchCameraLabel());
        switchCameraButton.setOnAction(e -> {
            cameraDevice = (cameraDevice + 1) % CAMERA_DEVICES;
            switchCameraButton.setText(switchCameraLabel());
            if (livePreviewActive || cameraSource != null) startCameraPreview(cameraDevice);
            else cameraStatus.setText("Camera " + cameraDevice + " will be used when you start the preview.");
        });
        var begin = new Button("Start Steady Start"); begin.getStyleClass().add("primary"); begin.setOnAction(e -> beginRoutine());
        var review = new Button("Review licensed pose candidates"); review.setOnAction(e -> showPoseGallery());
        var left = new VBox(18, title, guide, badge, cameraStatus, note, new HBox(10, cameraButton, switchCameraButton), new HBox(10, begin, review)); left.setMaxWidth(520);
        var page = new BorderPane(previewStack, null, null, null, left); page.setPadding(new Insets(50)); BorderPane.setMargin(left, new Insets(0, 35, 0, 0)); page.getStyleClass().add("page");
        drawFrame(landmarks.nextFrame()); setPage(page);
    }

    private void startCameraPreview(int deviceIndex) {
        stopCameraPreview();
        cameraStatus.setText("Opening camera " + deviceIndex + " locally...");
        cameraStatus.getStyleClass().add("camera-status-active");
        cameraButton.setDisable(true);
        cameraButton.setText("Opening camera...");
        var startup = CameraLandmarkSource.open(deviceIndex);
        modelFallbackReason = describe(startup);
        if (startup instanceof CameraLandmarkSource.Startup.Ready ready) {
            cameraSource = ready.source();
            landmarks = cameraSource;
            landmarks.selectPose(routine == null ? "easy_seat" : current().pose().id());
            cameraSource.start(this::showCameraFrame,
                    message -> Platform.runLater(() -> cameraStatus.setText(message)),
                    message -> Platform.runLater(() -> {
                        cameraPreview.setVisible(false);
                        if (landmarkOverlay != null) landmarkOverlay.getChildren().clear();
                        bodyView.setVisible(true);
                        cameraStatus.setText(message + " Demo mode remains available.");
                        cameraButton.setDisable(false);
                        cameraButton.setText("Try camera again");
                        restoreDemoSource();
                    }));
            startOpenTimeout();
            return;
        }
        // Falling back to a picture-only preview is fine, but it must say why:
        // this screen used to claim "the pose model is not connected yet"
        // whether the weights were absent or present and broken.
        cameraStatus.setText(modelFallbackReason);
        cameraCapture = CameraCapture.forDevice(deviceIndex);
        cameraCapture.start(this::showCameraFrame,
                message -> Platform.runLater(() -> cameraStatus.setText(message)),
                message -> Platform.runLater(() -> {
            cameraPreview.setVisible(false);
            bodyView.setVisible(true);
            cameraStatus.setText(message + " Demo mode remains available.");
            cameraButton.setDisable(false);
            cameraButton.setText("Try camera again");
        }));
        startOpenTimeout();
    }

    /** Why landmark estimation is off, in words someone can act on. */
    private String describe(CameraLandmarkSource.Startup startup) {
        return switch (startup) {
            case CameraLandmarkSource.Startup.Ready ignored -> "";
            case CameraLandmarkSource.Startup.NoModel none ->
                    "No pose model installed, so the picture is shown but not measured. Run scripts\\fetch-model.ps1 to enable it. Looked in: "
                            + none.searched().stream().map(Path::toString).reduce((a, b) -> a + "; " + b).orElse("nowhere");
            case CameraLandmarkSource.Startup.Unusable broken ->
                    "A pose model is installed at " + broken.model() + " but could not be loaded, so the picture is shown but not measured: "
                            + broken.reason() + " Re-run scripts\\fetch-model.ps1 to replace it.";
        };
    }

    private String switchCameraLabel() { return "Using camera " + cameraDevice + " · switch"; }

    private void startOpenTimeout() {
        cameraOpenTimeout = new PauseTransition(Duration.seconds(8));
        cameraOpenTimeout.setOnFinished(e -> {
            boolean open = cameraSource != null ? cameraSource.isOpen() : cameraCapture != null && cameraCapture.isOpen();
            if (!open) {
                cameraStatus.setText("The camera is taking longer than expected to open. Check Settings > Privacy & security > Camera, close other camera apps, then try again.");
                cameraButton.setDisable(false);
                cameraButton.setText("Try camera again");
            }
        });
        cameraOpenTimeout.play();
    }

    private void restoreDemoSource() {
        landmarks = demoSource;
        cameraSource = null;
        if (routine != null) demoSource.selectPose(current().pose().id());
    }

    private void showCameraFrame(CameraFrame frame) {
        pendingCameraFrame.set(frame);
        if (!cameraRenderPending.compareAndSet(false, true)) return;
        Platform.runLater(() -> {
            var latest = pendingCameraFrame.getAndSet(null);
            if (cameraPreview != null && latest != null) {
                var image = new WritableImage(latest.width(), latest.height());
                image.getPixelWriter().setPixels(0, 0, latest.width(), latest.height(),
                        PixelFormat.getByteBgraInstance(), forDisplay(latest), 0, latest.width() * 4);
                livePreviewActive = true;
                if (figureOnly && cameraSource != null) {
                    // the picture is decoded but never shown: only the figure
                    // drawn from it reaches the screen
                    cameraPreview.setVisible(false);
                    bodyView.setVisible(true);
                    if (landmarkOverlay != null) landmarkOverlay.getChildren().clear();
                    drawFrame(landmarks.nextFrame(), true);
                } else {
                    cameraPreview.setImage(image);
                    cameraPreview.setVisible(true);
                    bodyView.setVisible(false);
                    if (cameraSource != null) drawCameraOverlay(landmarks.nextFrame(), latest.width());
                }
                cameraStatus.setText(cameraSource != null
                        ? "Camera active. Your landmarks are estimated on this device, frame by frame; nothing is recorded or uploaded."
                        : "Local preview active. Frames are transient and are not saved. Alignment analysis is still demo-only.");
                cameraButton.setDisable(false);
                cameraButton.setText("Restart camera preview");
            }
            cameraRenderPending.set(false);
            var next = pendingCameraFrame.get();
            if (next != null) showCameraFrame(next);
        });
    }

    private void stopCameraPreview() {
        if (cameraCapture != null) cameraCapture.close();
        cameraCapture = null;
        if (cameraSource != null) cameraSource.close();
        restoreDemoSource();
        if (landmarkOverlay != null) landmarkOverlay.getChildren().clear();
        livePreviewActive = false;
        if (cameraOpenTimeout != null) cameraOpenTimeout.stop();
        cameraOpenTimeout = null;
        pendingCameraFrame.set(null);
    }

    private void showPoseGallery() {
        var title = new Label("Teaching pose review"); title.getStyleClass().add("title");
        var intro = new Label("CC0 reference candidates are credited and kept separate from camera observations. They remain disabled during coaching until final visual approval."); intro.setWrapText(true); intro.setMaxWidth(1050); intro.getStyleClass().add("lead");
        var candidatesTitle = new Label("LICENSED REFERENCE CANDIDATES"); candidatesTitle.getStyleClass().add("badge");
        var candidates = new TilePane(); candidates.setHgap(18); candidates.setVgap(18); candidates.setPrefColumns(2); candidates.setPrefTileWidth(370);
        teachingAssets.reviewCandidates().stream().map(this::teachingAssetCard).forEach(candidates.getChildren()::add);
        var back = new Button("Back to camera setup"); back.getStyleClass().add("primary"); back.setOnAction(e -> showCalibration());
        var page = new VBox(18, title, intro, candidatesTitle, candidates);
        if (getParameters().getRaw().contains("--pose-gallery")) {
            var draftsTitle = new Label("DEVELOPER DIAGNOSTIC DRAFTS · REJECTED FOR COACHING"); draftsTitle.getStyleClass().add("badge");
            var gallery = new TilePane(); gallery.setHgap(18); gallery.setVgap(18); gallery.setPrefColumns(3); gallery.setPrefTileWidth(370); gallery.setPrefTileHeight(430);
            for (var draft : teachingDrafts.all()) gallery.getChildren().add(poseDraftCard(draft));
            page.getChildren().addAll(draftsTitle, gallery);
        }
        page.getChildren().add(back); page.setPadding(new Insets(35)); page.getStyleClass().add("page");
        setPage(scrollable(page));
    }

    private VBox teachingAssetCard(TeachingAsset asset) {
        var name = new Label(asset.displayName()); name.getStyleClass().add("teaching-pose-name");
        var stream = Objects.requireNonNull(SahaApp.class.getResourceAsStream(asset.resourcePath()));
        var image = new Image(stream);
        var art = new ImageView(image); art.setFitWidth(330); art.setFitHeight(270); art.setPreserveRatio(true);
        var artPane = new StackPane(art); artPane.getStyleClass().add("licensed-art-canvas"); artPane.setPrefSize(340, 280);
        var credit = new Label(asset.licenseName() + " · " + asset.creator()); credit.getStyleClass().add("teaching-review");
        var state = new Label("REVIEWED CANDIDATE · COACHING USE OFF"); state.setWrapText(true); state.getStyleClass().add("visual-review-warning");
        var note = new Label(asset.reviewNote()); note.setWrapText(true); note.getStyleClass().add("support-label");
        return new VBox(7, name, credit, artPane, state, note);
    }

    private VBox poseDraftCard(TeachingPoseDraft draft) {
        var name = new Label(draft.displayName()); name.getStyleClass().add("teaching-pose-name");
        var view = new Label(draft.view() + " · gaze: " + draft.gaze()); view.getStyleClass().add("teaching-review");
        var art = new TeachingPoseDraftView(draft); art.getStyleClass().add("pose-draft-canvas");
        var contacts = new Label("Grounding check: both feet that should be down are on the floor."); contacts.setWrapText(true); contacts.getStyleClass().add("support-label");
        var card = new VBox(7, name, view, art, contacts); card.getStyleClass().add("pose-draft-card");
        return card;
    }

    private HBox check(String value) { var dot = new Label("✓"); dot.getStyleClass().add("check"); var text = new Label(value); text.setWrapText(true); return new HBox(10, dot, text); }

    private void beginRoutine() {
        try { var recommendation = new PersonalizationEngine().recommend(store.load()); routine = generator.beginner(recommendation.durationAdjustments(), recommendation.explanations(), preferredIntensity); }
        catch (IOException e) { routine = generator.beginner(Map.of(), List.of("Session history could not be read; using the gentle baseline."), preferredIntensity); }
        if (speechWanted && !voice.isAvailable()) voice = SystemVoice.create();
        itemIndex = 0; remaining = routine.items().getFirst().durationSeconds(); showCoach();
    }

    private void showCoach() {
        poseLabel = new Label(); poseLabel.setWrapText(true); poseLabel.setMinHeight(Region.USE_PREF_SIZE); poseLabel.getStyleClass().add("hero-small"); phaseLabel = new Label(); phaseLabel.getStyleClass().add("badge");
        statusLabel = wrapLabel(); statusLabel.getStyleClass().add("status"); suggestionLabel = wrapLabel(); optionalLabel = wrapLabel(); confidenceLabel = wrapLabel(); timerLabel = new Label(); timerLabel.getStyleClass().add("timer");
        measurementLabel = wrapLabel(); measurementLabel.getStyleClass().add("measurement");
        // tall enough to hold the card it is given: below this the illustration
        // and the licence badge drew outside the card, over the panel beneath
        teachingView = new VBox(8); teachingView.setPrefSize(560, 215); teachingView.setMinHeight(200); teachingView.setMaxHeight(230); teachingView.getStyleClass().add("teaching-view");
        bodyView = createBodyView(); bodyView.setPrefSize(560, 420);
        cameraPreview = new ImageView(); cameraPreview.setPreserveRatio(true); cameraPreview.setVisible(livePreviewActive);
        cameraPreview.setScaleX(mirrorPreview ? -1 : 1);
        landmarkOverlay = createOverlay();
        var observationView = new StackPane(bodyView, cameraPreview, landmarkOverlay);
        observationView.setPrefSize(560, 420); observationView.setMinHeight(260); observationView.getStyleClass().add("camera-observation");
        // the video is the point once the camera is on, so let it take the room
        // the window has rather than sitting in a fixed letterbox
        cameraPreview.fitWidthProperty().bind(observationView.widthProperty().subtract(12));
        cameraPreview.fitHeightProperty().bind(observationView.heightProperty().subtract(12));
        bodyView.setVisible(!livePreviewActive);
        var stop = actionButton("Stop now"); stop.getStyleClass().add("danger"); stop.setOnAction(e -> finish(false));
        var pause = actionButton("Pause"); pause.setOnAction(e -> { paused = !paused; pause.setText(paused ? "Resume" : "Pause"); });
        var repeat = actionButton("Repeat cue"); repeat.setOnAction(e -> {
            suggestionLabel.requestFocus();
            voice.say(suggestionLabel.getText());
        });
        var easier = actionButton("Easier option"); easier.setOnAction(e -> optionalLabel.setText("Optional adjustment: " + current().pose().modifications().getFirst()));
        var next = actionButton("Next pose"); next.getStyleClass().add("next"); next.setOnAction(e -> advance(true));
        var tint = actionButton(figureOnly ? "View: figure only" : "View: camera picture");
        tint.setOnAction(e -> {
            figureOnly = !figureOnly;
            tint.setText(figureOnly ? "View: figure only" : "View: camera picture");
            if (figureOnly && landmarkOverlay != null) landmarkOverlay.getChildren().clear();
        });
        var speech = actionButton(speechLabel());
        speech.setOnAction(e -> {
            speechWanted = !speechWanted;
            if (speechWanted && !voice.isAvailable()) voice = SystemVoice.create();
            else if (!speechWanted) { voice.close(); voice = Voice.silent(); }
            chime.setMuted(!speechWanted);
            speech.setText(speechLabel());
        });
        var controls = new GridPane(); controls.setHgap(10); controls.setVgap(10);
        var leftColumn = new ColumnConstraints(); leftColumn.setPercentWidth(50);
        var rightColumn = new ColumnConstraints(); rightColumn.setPercentWidth(50);
        controls.getColumnConstraints().addAll(leftColumn, rightColumn);
        controls.add(pause, 0, 0); controls.add(repeat, 1, 0);
        controls.add(easier, 0, 1); controls.add(next, 1, 1);
        controls.add(speech, 0, 2); controls.add(tint, 1, 2);
        diagnosticsLabel = wrapLabel();
        diagnosticsLabel.setVisible(false); diagnosticsLabel.setManaged(false);
        if (cameraSource != null) {
            // Only a real camera has anything to diagnose: the demo source has
            // no model, no crop and no capture rate worth a number.
            var diagnose = actionButton("Diagnostics: off");
            diagnose.setOnAction(e -> {
                showDiagnostics = !showDiagnostics;
                diagnose.setText(showDiagnostics ? "Diagnostics: on" : "Diagnostics: off");
                diagnosticsLabel.setVisible(showDiagnostics); diagnosticsLabel.setManaged(showDiagnostics);
                if (!showDiagnostics) diagnosticsLabel.setText("");
            });
            controls.add(diagnose, 0, 3, 2, 1);
        }
        controls.add(stop, 0, cameraSource != null ? 4 : 3, 2, 1);
        var reasonText = wrapLabel(); reasonText.setMinHeight(Region.USE_PREF_SIZE); reasonText.setText(String.join(" ", routine.explanations()));
        var feedback = new VBox(10, phaseLabel, poseLabel, timerLabel, statusLabel, measurementLabel, suggestionLabel, optionalLabel, confidenceLabel, diagnosticsLabel, new Separator(), new Label("Why this routine changed"), reasonText, controls); feedback.getStyleClass().add("card"); feedback.setPrefWidth(400); feedback.setMaxWidth(430);
        var observationTitle = new Label(cameraSource != null
                ? "YOU, AS THE COACH SEES YOU · NO ROOM, NO FURNITURE"
                : livePreviewActive
                ? "LIVE CAMERA PREVIEW · ALIGNMENT NOT YET ANALYZED"
                : "SYNTHETIC DEMO LANDMARKS · NOT AN EXAMPLE POSE"); observationTitle.getStyleClass().add("observation-title");
        var observation = new VBox(5, observationTitle, observationView); VBox.setVgrow(observationView, Priority.ALWAYS);
        var visualColumn = new VBox(12, teachingView, observation); VBox.setVgrow(teachingView, Priority.NEVER); VBox.setVgrow(observation, Priority.ALWAYS);
        var coach = new BorderPane(visualColumn, null, feedback, null, null); BorderPane.setMargin(feedback, new Insets(0, 0, 0, 25));
        practicePath = new HBox(8); practicePath.setAlignment(Pos.CENTER_LEFT);
        practicePathScroll = new ScrollPane(practicePath); practicePathScroll.setFitToHeight(true); practicePathScroll.setPannable(true); practicePathScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); practicePathScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); practicePathScroll.getStyleClass().add("practice-path-scroll");
        var pathTitle = new Label("TODAY'S PRACTICE PATH"); pathTitle.getStyleClass().add("badge");
        var pathArea = new VBox(7, pathTitle, practicePathScroll); pathArea.getStyleClass().add("practice-path-area");
        var page = new VBox(18, pathArea, coach); VBox.setVgrow(coach, Priority.ALWAYS); page.setPadding(new Insets(24, 35, 35, 35)); page.getStyleClass().add("page");
        setPage(page); updatePose();
        clockTicks = 0;
        clock = new Timeline(new KeyFrame(Duration.millis(100), e -> tick())); clock.setCycleCount(Timeline.INDEFINITE); clock.play();
    }

    private Label wrapLabel() { var label = new Label(); label.setWrapText(true); label.setMaxWidth(Double.MAX_VALUE); label.setMinHeight(Region.USE_PREF_SIZE); return label; }
    private Button actionButton(String text) { var button = new Button(text); button.setMaxWidth(Double.MAX_VALUE); return button; }
    private RoutineItem current() { return routine.items().get(itemIndex); }
    private void speakSetup() { spoken.setup(current().pose()).ifPresent(voice::say); }
    private void tick() {
        if (livePreviewActive && cameraSource == null) {
            statusLabel.setText("Status: Live preview — alignment not analyzed");
            suggestionLabel.setText("Guidance: Follow the written teaching guide. "
                    + (modelFallbackReason.isEmpty() ? "Saha can display your camera, but the pose model is not connected yet." : modelFallbackReason));
            optionalLabel.setText("Optional adjustment: " + current().pose().modifications().getFirst());
            confidenceLabel.setText("Camera: Active · alignment confidence unavailable");
            speakSetup();
            if (!paused && ++clockTicks % 10 == 0 && --remaining <= 0) advance(false);
            timerLabel.setText(format(remaining) + (paused ? " · paused" : ""));
            return;
        }
        var frame = landmarks.nextFrame();
        // before the analysis, so getting into the pose is talked through while
        // there is still time to act on it - and whether or not it is measured
        speakSetup();
        if (cameraSource != null && !figureOnly) cameraSource.latestImage().ifPresent(image -> drawCameraOverlay(frame, image.width()));
        else drawFrame(frame, cameraSource != null);
        if (showDiagnostics && cameraSource != null) cameraSource.diagnostics().ifPresent(d -> diagnosticsLabel.setText(describe(d)));
        if (landmarks.isTransitioning()) {
            statusLabel.setText("Status: Moving into " + current().pose().displayName());
            suggestionLabel.setText("Transition: " + landmarks.transitionGuidance());
            optionalLabel.setText("The hold begins when the transition finishes.");
            confidenceLabel.setText("Analysis: Paused during transition");
            timerLabel.setText(format(remaining) + " · transition");
            return;
        }
        var result = analyzer.analyze(current().pose(), frame);
        boolean mayTime = result instanceof AnalysisResult.Reliable || result instanceof AnalysisResult.InstructionOnly;
        switch (result) {
            case AnalysisResult.Reliable r -> {
                statusLabel.setText("Status: " + r.status());
                suggestionLabel.setText("Primary suggestion: " + (r.suggestions().isEmpty() ? "Keep breathing comfortably." : r.suggestions().getFirst()));
                spoken.framingResolved();
                measurementLabel.setText(readMeasurements(r));
                boolean aligned = r.suggestions().isEmpty() && r.measurements().stream().anyMatch(AnalysisResult.Measurement::inRange);
                // the chime says something happened; the words say what
                if (aligned && !wasAligned) {
                    chime.play();
                    spoken.arrived().ifPresent(voice::say);
                }
                wasAligned = aligned;
                if (!r.suggestions().isEmpty()) spoken.cue(r.suggestions().getFirst()).ifPresent(voice::say);
                else if (aligned) spoken.praise().ifPresent(voice::say);
                optionalLabel.setText("Optional adjustment: " + current().pose().modifications().getFirst());
                confidenceLabel.setText("Confidence: " + level(r.confidence()) + reading(r.confidence()));
            }
            case AnalysisResult.InstructionOnly instruction -> {
                spoken.framingResolved();
                measurementLabel.setText("");
                statusLabel.setText("Status: Instruction only — alignment not measured");
                suggestionLabel.setText("Guidance: " + instruction.guidance());
                optionalLabel.setText("Optional adjustment: " + current().pose().modifications().getFirst());
                confidenceLabel.setText("Camera visibility: " + level(instruction.confidence()));
            }
            case AnalysisResult.Unreliable u -> {
                statusLabel.setText("Status: Camera view needs attention"); suggestionLabel.setText("Primary suggestion: " + u.guidance());
                measurementLabel.setText("");
                optionalLabel.setText("Corrections are paused until the view improves.");
                confidenceLabel.setText("Confidence: " + level(u.confidence()) + reading(u.confidence()));
                spoken.framing(u.guidance()).ifPresent(voice::say);
            }
        }
        if (!paused && mayTime && ++clockTicks % 10 == 0 && --remaining <= 0) advance(false);
        callTheChangeOfSides();
        timerLabel.setText(format(remaining) + (paused || !mayTime ? " · paused" : ""));
    }

    /**
     * Halfway through a one-sided pose, say so.
     *
     * The routine holds each of these once, so without this the practice works
     * one leg and leaves the other. The check is an equality on the second it
     * crosses the midpoint, which happens once per hold.
     */
    private void callTheChangeOfSides() {
        if (routine == null || !catalog.isOneSided(current().pose().id())) return;
        if (remaining != current().durationSeconds() / 2) return;
        spoken.switchSides().ifPresent(voice::say);
        // the shape to move into turns around too: it was still showing the
        // side that had just finished, so it asked for the leg you were leaving
        demoSource.switchSides();
        optionalLabel.setText("Switch sides — the second half of this hold works the other side.");
    }
    /**
     * The angles measured this frame, with their targets.
     *
     * Showing the live number is the clearest answer to "is it actually seeing
     * me?" — it moves as you move. An ungraded measurement shows its value with
     * no verdict, because for a shape like cat–cow no single value is right.
     */
    private String readMeasurements(AnalysisResult.Reliable result) {
        if (result.measurements().isEmpty()) return "";
        return result.measurements().stream()
                .map(m -> m.graded()
                        ? "%s %.0f° %s target %.0f–%.0f°".formatted(m.label(), m.degrees(), m.inRange() ? "✓" : "·", m.minimum(), m.maximum())
                        : "%s %.0f° · watching".formatted(m.label(), m.degrees()))
                .reduce((a, b) -> a + "\n" + b).orElse("");
    }

    private String speechLabel() {
        if (!speechWanted) return "Spoken guidance: off";
        return voice.isAvailable() ? "Spoken guidance: on" : "Spoken guidance unavailable";
    }
    private String level(double value) { return value >= .85 ? "High" : value >= .70 ? "Medium" : "Low"; }

    /**
     * The diagnostics line: raw scores, the crop, and the pace.
     *
     * <p>Scores are the model's own, before smoothing, because the question
     * this view answers is what the model sees - the drawn figure already
     * shows what the smoother makes of it. Left|Right, in camera truth, not
     * mirrored: the readout matches the data, and the mirrored preview is a
     * courtesy for bodies, not for numbers.
     */
    private static String describe(VisionDiagnostics d) {
        var raw = d.raw().landmarks();
        return "Raw scores L|R — shoulder " + pair(raw, LandmarkName.LEFT_SHOULDER, LandmarkName.RIGHT_SHOULDER)
                + " · hip " + pair(raw, LandmarkName.LEFT_HIP, LandmarkName.RIGHT_HIP)
                + " · wrist " + pair(raw, LandmarkName.LEFT_WRIST, LandmarkName.RIGHT_WRIST)
                + " · knee " + pair(raw, LandmarkName.LEFT_KNEE, LandmarkName.RIGHT_KNEE)
                + " · ankle " + pair(raw, LandmarkName.LEFT_ANKLE, LandmarkName.RIGHT_ANKLE)
                + "\n" + (d.wholeFrame() ? "Crop: whole frame"
                        : "Crop: %.2f of frame width, from (%.2f, %.2f)".formatted(d.regionSize(), d.regionX(), d.regionY()))
                + " · estimate %.0f ms · %.0f landmark frames/s".formatted(d.estimateMillis(), d.landmarksPerSecond());
    }

    private static String pair(java.util.Map<LandmarkName, Landmark> raw, LandmarkName left, LandmarkName right) {
        return score(raw.get(left)) + "|" + score(raw.get(right));
    }

    private static String score(Landmark mark) { return mark == null ? "—" : "%.2f".formatted(mark.confidence()); }
    /** The measured number alongside the word, so a pause can be diagnosed rather than guessed at. */
    private String reading(double value) { return cameraSource == null ? "" : " (%.2f, needs %.2f)".formatted(value, PoseAnalyzer.RELIABILITY_THRESHOLD); }
    private String format(int seconds) { return "%d:%02d".formatted(seconds / 60, seconds % 60); }
    private void updatePose() {
        var item = current();
        landmarks.selectPose(item.pose().id());
        // the guide figure is drawn from the demo source's authored target, so
        // it has to follow the practice even when the camera is driving
        if (landmarks != demoSource) demoSource.selectPose(item.pose().id());
        poseLabel.setText("Current pose: " + item.pose().displayName());
        phaseLabel.setText(item.phase().toUpperCase()); timerLabel.setText(format(remaining));
        spoken.announce(item.pose()).ifPresent(voice::say);
        wasAligned = false;
        updateTeachingView(item);
        updatePracticePath();
        if (!livePreviewActive) drawFrame(landmarks.nextFrame());
        confidenceLabel.setText(cameraSource != null ? "Camera: Active · your landmarks" : confidenceLabel.getText());
    }
    private void updateTeachingView(RoutineItem item) {
        if (teachingView == null) return;
        teachingView.getChildren().clear();
        var heading = new Label("TEACHING GUIDE"); heading.getStyleClass().add("badge");
        // the pose name is already the headline of the feedback panel beside
        // this card, so here it is a caption rather than a second hero
        var title = new Label(item.pose().displayName()); title.getStyleClass().addAll("teaching-pose-name", "compact");
        var instruction = new Label(item.pose().instructions().getFirst()); instruction.setWrapText(true); instruction.setMinHeight(Region.USE_PREF_SIZE); instruction.getStyleClass().add("teaching-instruction");
        var icon = poseIcons.forPose(item.pose().id());
        var asset = icon.isPresent() ? Optional.<TeachingAsset>empty() : teachingAssets.enabledForCoaching(item.pose().id());
        boolean illustrated = icon.isPresent() || asset.isPresent();
        var status = illustrations.status(item.pose().id());
        // Provenance and floor contact are one footnote rather than two stacked
        // lines. Every claim the card used to make it still makes; five separate
        // blocks simply did not fit the height coaching can spare, and the
        // illustration was spilling past the card onto the panel below.
        var provenance = new Label(asset.map(TeachingAsset::reviewNote)
                .orElseGet(() -> status.map(value -> value.requiredView() + " · visual " + value.reviewState().name().toLowerCase().replace('_', ' ')).orElse("Written guidance only · illustration not yet reviewed"))
                + " · " + status.map(value -> "on the floor: " + value.grounding().requiredContacts().stream().map(contact -> contact.name().toLowerCase().replace('_', ' ')).sorted().reduce((a, b) -> a + ", " + b).orElse("not defined")).orElse("floor contact still being defined"));
        provenance.setWrapText(true); provenance.setMinHeight(Region.USE_PREF_SIZE); provenance.getStyleClass().add("teaching-review");
        var boundary = new Label(illustrated
                ? "License-verified reference illustration"
                : "Illustration under review. Follow the written setup or skip this pose.");
        boundary.setWrapText(true); boundary.setMinHeight(Region.USE_PREF_SIZE); boundary.getStyleClass().add(illustrated ? "visual-approved" : "visual-review-warning");
        var text = new VBox(6, title, instruction, provenance, boundary);
        HBox.setHgrow(text, Priority.ALWAYS);
        var body = new HBox(14, text);
        icon.ifPresent(value -> {
            var view = new PoseIconView();
            view.show(value);
            view.setMinSize(120, 120); view.setPrefSize(130, 130);
            // the credit wraps rather than ellipsing: an attribution cut off at
            // "Atlas Icons · Ramy ..." is not the attribution the licence asks for
            var credit = new Label(PoseIconCatalog.CREDIT); credit.getStyleClass().add("support-label");
            credit.setWrapText(true); credit.setMinHeight(Region.USE_PREF_SIZE); credit.setMaxWidth(140);
            var iconColumn = new VBox(4, view, credit); iconColumn.setAlignment(Pos.CENTER);
            body.getChildren().add(iconColumn);
        });
        asset.ifPresent(value -> {
            var stream = Objects.requireNonNull(SahaApp.class.getResourceAsStream(value.resourcePath()));
            var art = new ImageView(new Image(stream));
            art.setPreserveRatio(true); art.setFitWidth(150); art.setFitHeight(125);
            var artPane = new StackPane(art); artPane.getStyleClass().add("licensed-art-canvas");
            var credit = new Label("CC0 · " + value.creator()); credit.getStyleClass().add("support-label");
            var artColumn = new VBox(4, artPane, credit); artColumn.setAlignment(Pos.CENTER);
            body.getChildren().add(artColumn);
        });
        VBox.setVgrow(body, Priority.ALWAYS);
        // The illustration draws the floor it actually rests on; a second rule
        // across the whole card sat at a different height and contradicted it.
        teachingView.getChildren().addAll(heading, body);
    }
    private void updatePracticePath() {
        if (practicePath == null) return;
        practicePath.getChildren().clear();
        for (int i = 0; i < routine.items().size(); i++) {
            var item = routine.items().get(i);
            if (i > 0) {
                var arrow = new Label("→"); arrow.getStyleClass().add("path-arrow");
                practicePath.getChildren().add(arrow);
            }
            var marker = new Label(i == itemIndex ? "YOU ARE HERE" : item.phase().toUpperCase()); marker.getStyleClass().add("path-marker");
            var name = new Label(item.pose().displayName()); name.setWrapText(true); name.getStyleClass().add("path-name");
            var duration = new Label(format(item.durationSeconds())); duration.getStyleClass().add("path-duration");
            var visualStatus = new Label(poseIcons.forPose(item.pose().id()).isPresent()
                    || teachingAssets.enabledForCoaching(item.pose().id()).isPresent() ? "REFERENCE VISUAL" : "WRITTEN GUIDE"); visualStatus.getStyleClass().add("path-visual-status");
            var card = new VBox(2, marker, name, duration, visualStatus); card.getStyleClass().add("path-pose");
            if (i < itemIndex) card.getStyleClass().add("complete");
            if (i == itemIndex) card.getStyleClass().add("current");
            practicePath.getChildren().add(card);
        }
        double location = routine.items().size() <= 1 ? 0 : (double) itemIndex / (routine.items().size() - 1);
        Platform.runLater(() -> practicePathScroll.setHvalue(location));
    }
    private void advance(boolean skipped) {
        saveMetric(skipped); if (++itemIndex >= routine.items().size()) { finish(true); return; }
        remaining = current().durationSeconds(); updatePose();
    }
    private void saveMetric(boolean skipped) {
        try { store.append(new SessionMetric(current().pose().id(), Instant.now(), current().durationSeconds() - remaining, skipped ? 0 : .82, 1, skipped, .91, true)); } catch (IOException ignored) { }
    }
    private void finish(boolean completed) {
        if (clock != null) clock.stop();
        stopCameraPreview();
        spoken.finish(completed).ifPresent(voice::say);
        if (completed) chime.play();
        showProgress(completed);
    }

    private void showProgress(boolean completed) {
        List<SessionMetric> history; try { history = store.load(); } catch (IOException e) { history = List.of(); }
        long completedPoses = history.stream().filter(m -> !m.skipped()).count();
        double stability = history.stream().mapToDouble(SessionMetric::stability).average().orElse(0);
        var title = new Label(completed ? "You finished the whole practice" : "Practice stopped"); title.getStyleClass().add("hero");
        var stats = new HBox(16, stat("POSES COMPLETED", Long.toString(completedPoses)), stat("AVERAGE STABILITY", Math.round(stability * 100) + "%"), stat("LOCAL RECORDS", Integer.toString(history.size())));
        var privacy = new Label("Only derived session metrics are stored locally. No images or landmark coordinates are saved."); privacy.setWrapText(true);
        var delete = new Button("Delete all local history"); delete.setOnAction(e -> { try { store.deleteAll(); showProgress(completed); } catch (IOException ex) { privacy.setText("Could not delete local history: " + ex.getMessage()); } });
        var again = new Button("Plan another practice"); again.getStyleClass().add("primary"); again.setOnAction(e -> showWelcome());
        var page = new VBox(24, title, new Label("Progress, without body scoring."), stats, privacy, new HBox(10, again, delete)); page.setAlignment(Pos.CENTER_LEFT); page.setPadding(new Insets(80)); page.getStyleClass().add("page"); setPage(page);
    }
    private VBox stat(String label, String value) { var number = new Label(value); number.getStyleClass().add("stat-number"); var box = new VBox(7, new Label(label), number); box.getStyleClass().add("card"); HBox.setHgrow(box, Priority.ALWAYS); return box; }

    private LandmarkFrame lastDrawnFrame;
    /** Bones drawn over the live video; the same links the demo figure uses. */
    private static final List<LandmarkName[]> LINKS = List.of(
            new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.RIGHT_SHOULDER},
            new LandmarkName[]{LandmarkName.LEFT_HIP,LandmarkName.RIGHT_HIP},
            new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.LEFT_HIP},
            new LandmarkName[]{LandmarkName.RIGHT_SHOULDER,LandmarkName.RIGHT_HIP},
            new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.LEFT_ELBOW},
            new LandmarkName[]{LandmarkName.LEFT_ELBOW,LandmarkName.LEFT_WRIST},
            new LandmarkName[]{LandmarkName.RIGHT_SHOULDER,LandmarkName.RIGHT_ELBOW},
            new LandmarkName[]{LandmarkName.RIGHT_ELBOW,LandmarkName.RIGHT_WRIST},
            new LandmarkName[]{LandmarkName.LEFT_HIP,LandmarkName.LEFT_KNEE},
            new LandmarkName[]{LandmarkName.LEFT_KNEE,LandmarkName.LEFT_ANKLE},
            new LandmarkName[]{LandmarkName.RIGHT_HIP,LandmarkName.RIGHT_KNEE},
            new LandmarkName[]{LandmarkName.RIGHT_KNEE,LandmarkName.RIGHT_ANKLE});
    /** Below this, a keypoint is a guess rather than an observation, so it is not drawn. */
    private static final double DRAW_THRESHOLD = .30;
    /**
     * Below this a joint is not drawn at all.
     *
     * The model reports all seventeen keypoints on every frame whether or not
     * it can see them, so there is always a number. This is the point below
     * which that number carries no information worth showing.
     */
    private static final double GHOST_FLOOR = .08;

    /**
     * How solidly to draw a joint, from its confidence.
     *
     * Sitting cross-legged folds both knees and both ankles behind each other
     * and the model scores them under the drawing threshold however well you
     * are sitting, so dropping the limb outright left the practice opening on a
     * figure with no legs. Drawing it faintly says "roughly here, not certain",
     * which is true, and leaves a whole body on screen.
     *
     * <p>The fade is continuous rather than a step at the threshold. A step
     * makes any joint hovering near the line flip between solid and faint
     * several times a second, which is its own kind of broken; ramping down to
     * nothing at the floor means a limb fades out instead of blinking out.
     *
     * <p>Nothing downstream reads this. The measurement gate is separate and
     * unchanged, so a guessed leg still cannot earn a correction or a chime.
     */
    private static double visibility(double confidence) {
        if (confidence >= DRAW_THRESHOLD) return 1;
        if (confidence <= GHOST_FLOOR) return 0;
        return (confidence - GHOST_FLOOR) / (DRAW_THRESHOLD - GHOST_FLOOR);
    }

    /** A limb is only as certain as its least certain end. */
    private static double visibility(double a, double b) { return visibility(Math.min(a, b)); }

    // A friendlier figure: warm lit joints, rounded limbs, and a face that
    // looks back at you. Someone holding a pose for fifty seconds is looking at
    // this, so it may as well be good company.
    // Neon: the body you are moving should be the brightest thing on screen.
    private static final Color LIMB = Color.web("#2bf5cf");
    private static final Color JOINT = Color.web("#ffc94a");
    /** The head carries the joints' own light, so the face reads as lit rather than painted on. */
    private static final Color SKIN = JOINT;
    private static final Color INK = Color.web("#10322e");
    /**
     * The target shape, deliberately dim.
     *
     * It was competing with the live body for attention, which inverted the
     * point: the thing you are doing should be what catches the eye, and the
     * target should be a quiet suggestion behind it.
     */
    private static final Color GUIDE = Color.web("#4d6f68", .40);
    /** The links the guide figure draws; hands and toes are left off so it stays a clean shape. */
    private static final List<LandmarkName[]> GUIDE_LINKS = List.of(
            new LandmarkName[]{LandmarkName.LEFT_ANKLE,LandmarkName.LEFT_TOE},
            new LandmarkName[]{LandmarkName.RIGHT_ANKLE,LandmarkName.RIGHT_TOE},
            new LandmarkName[]{LandmarkName.LEFT_WRIST,LandmarkName.LEFT_HAND},
            new LandmarkName[]{LandmarkName.RIGHT_WRIST,LandmarkName.RIGHT_HAND},
            new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.RIGHT_SHOULDER},
            new LandmarkName[]{LandmarkName.LEFT_HIP,LandmarkName.RIGHT_HIP},
            new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.LEFT_HIP},
            new LandmarkName[]{LandmarkName.RIGHT_SHOULDER,LandmarkName.RIGHT_HIP},
            new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.LEFT_ELBOW},
            new LandmarkName[]{LandmarkName.LEFT_ELBOW,LandmarkName.LEFT_WRIST},
            new LandmarkName[]{LandmarkName.RIGHT_SHOULDER,LandmarkName.RIGHT_ELBOW},
            new LandmarkName[]{LandmarkName.RIGHT_ELBOW,LandmarkName.RIGHT_WRIST},
            new LandmarkName[]{LandmarkName.LEFT_HIP,LandmarkName.LEFT_KNEE},
            new LandmarkName[]{LandmarkName.LEFT_KNEE,LandmarkName.LEFT_ANKLE},
            new LandmarkName[]{LandmarkName.RIGHT_HIP,LandmarkName.RIGHT_KNEE},
            new LandmarkName[]{LandmarkName.RIGHT_KNEE,LandmarkName.RIGHT_ANKLE});

    private static javafx.scene.effect.DropShadow glow(Color colour, double radius) {
        return new javafx.scene.effect.DropShadow(javafx.scene.effect.BlurType.GAUSSIAN, colour, radius, .38, 0, 0);
    }

    /**
     * Draws the estimated joints over the video in the image's own space.
     *
     * Landmarks are normalized by the frame width on both axes, so one factor
     * maps them onto the letterboxed image the view is showing; using the
     * height for y would slide every point off the body on a non-square frame.
     */
    private void drawCameraOverlay(LandmarkFrame frame, int frameWidth) {
        if (landmarkOverlay == null || cameraPreview.getImage() == null) return;
        landmarkOverlay.getChildren().clear();
        var bounds = cameraPreview.getBoundsInParent();
        double shown = bounds.getWidth();
        if (shown <= 0 || frameWidth <= 0) return;
        double originX = bounds.getMinX(), originY = bounds.getMinY();
        var points = frame.landmarks();
        for (var link : LINKS) {
            var a = points.get(link[0]);
            var b = points.get(link[1]);
            if (a == null || b == null) continue;
            double solidity = visibility(a.confidence(), b.confidence());
            if (solidity == 0) continue;
            var bone = new Line(screenX(a.x(), originX, shown), originY + a.y() * shown,
                    screenX(b.x(), originX, shown), originY + b.y() * shown);
            bone.setStroke(LIMB);
            bone.setStrokeWidth(4);
            bone.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            bone.setEffect(glow(LIMB.deriveColor(0, 1, 1, .5), 7));
            bone.setOpacity(solidity);
            landmarkOverlay.getChildren().add(bone);
        }
        points.forEach((name, mark) -> {
            double solidity = visibility(mark.confidence());
            if (solidity == 0) return;
            double radius = switch (name) { case LEFT_HAND, RIGHT_HAND, LEFT_TOE, RIGHT_TOE -> 3; default -> 5; };
            var dot = new Circle(screenX(mark.x(), originX, shown), originY + mark.y() * shown, radius, JOINT);
            dot.setEffect(glow(JOINT, radius * 2.4));
            dot.setOpacity(solidity);
            landmarkOverlay.getChildren().add(dot);
        });
        if (showDiagnostics && cameraSource != null) cameraSource.diagnostics().ifPresent(d -> {
            // The box is the crop making itself visible: hunting, clipping a
            // limb, or collapsing are all things a person can see at a glance
            // and numbers hide. No box means the model is seeing the whole
            // frame - the readout already says so in words.
            if (d.wholeFrame()) return;
            double x1 = screenX(d.regionX(), originX, shown), x2 = screenX(d.regionX() + d.regionSize(), originX, shown);
            // A region may legitimately hang off the frame, but its box must
            // not hang off the video onto the rest of the page.
            double left = Math.max(Math.min(x1, x2), originX);
            double right = Math.min(Math.max(x1, x2), originX + bounds.getWidth());
            double top = Math.max(originY + d.regionY() * shown, originY);
            double bottom = Math.min(originY + (d.regionY() + d.regionSize()) * shown, originY + bounds.getHeight());
            if (right <= left || bottom <= top) return;
            var box = new javafx.scene.shape.Rectangle(left, top, right - left, bottom - top);
            box.setFill(null);
            box.setStroke(Color.GOLD);
            box.setStrokeWidth(2);
            box.getStrokeDashArray().addAll(8.0, 6.0);
            landmarkOverlay.getChildren().add(box);
        });
    }

    /**
     * Maps a landmark's x onto the screen, mirrored.
     *
     * The preview is flipped so it behaves like a mirror: lift the arm on your
     * left and the figure's left arm lifts with it. The landmarks themselves
     * stay in true camera space, so nothing downstream has to know.
     */
    private double screenX(double normalizedX, double originX, double shown) {
        return mirrorPreview ? originX + shown - normalizedX * shown : originX + normalizedX * shown;
    }

    /** The pixels to show, tinted or not. Reuses one buffer so this costs nothing per frame. */
    private byte[] forDisplay(CameraFrame frame) {
        var source = frame.bgraView();
        if (!stylizedColour) return source;
        if (tintScratch == null || tintScratch.length != source.length) tintScratch = new byte[source.length];
        for (int i = 0; i + 3 < source.length; i += 4) {
            tintScratch[i] = source[i + 2];
            tintScratch[i + 1] = source[i + 1];
            tintScratch[i + 2] = source[i];
            tintScratch[i + 3] = source[i + 3];
        }
        return tintScratch;
    }

    private Pane createOverlay() {
        var pane = new Pane();
        pane.setMouseTransparent(true);
        pane.setPickOnBounds(false);
        return pane;
    }

    private Pane createBodyView() {
        var pane = new Pane();
        pane.setMinSize(420, 180);
        // The calibration screen draws exactly once, before layout has sized the
        // pane, so the figure lands at the 420x180 fallback scale. Redraw when the
        // real size arrives. (The coach screen redraws every tick regardless.)
        pane.widthProperty().addListener((ignored, oldValue, newValue) -> redrawLastFrame(pane));
        pane.heightProperty().addListener((ignored, oldValue, newValue) -> redrawLastFrame(pane));
        return pane;
    }
    private void redrawLastFrame(Pane pane) { if (pane == bodyView && lastDrawnFrame != null) drawFrame(lastDrawnFrame); }
    private void drawFrame(LandmarkFrame frame) { drawFrame(frame, false); }

    /**
     * Draws the figure from a landmark frame.
     *
     * When {@code live} is true the points came from the camera, so only
     * confident ones are drawn and the floor reference is left off, because a
     * real body is not grounded to the demo source's fixed plane. Nothing of
     * the room is drawn in either case — the figure *is* the person, and the
     * furniture behind them was never needed to coach a pose.
     */
    private void drawFrame(LandmarkFrame frame, boolean live) {
        if (bodyView == null) return;
        lastDrawnFrame = live ? null : frame;
        bodyView.getChildren().clear();
        double w = Math.max(420, bodyView.getWidth()), h = Math.max(180, bodyView.getHeight());
        double scale = Math.min(w * .92, h * .82);
        double offsetX = (w - scale) / 2;
        double top = (h - scale) / 2;
        if (live) {
            // a camera frame is normalized by width, so it fills only the upper
            // part of a square box; lift and centre it rather than stranding the
            // figure against the top edge
            double lowest = frame.landmarks().values().stream().mapToDouble(Landmark::y).max().orElse(1);
            top += (scale - scale * lowest) / 2;
        } else {
            // the same plane the landmark source grounds poses to, so the body
            // rests on this line instead of floating relative to it
            double floorY = top + scale * LandmarkSource.FLOOR_Y;
            var floor = new Line(offsetX + scale * .04, floorY, offsetX + scale * .96, floorY);
            floor.setStroke(Color.web("#6fa89d")); floor.setStrokeWidth(2); floor.getStrokeDashArray().addAll(8.0, 6.0);
            bodyView.getChildren().add(floor);
            var floorLabel = new Text(offsetX + scale * .05, floorY - 6, "floor"); floorLabel.setFill(Color.web("#86aaa3"));
            bodyView.getChildren().add(floorLabel);
        }
        final double offsetY = top;
        // A live figure is mirrored, so the arm you raise is the arm that rises
        // on screen. The demo figure already faces you and needs no flip. The
        // camera overlay always mirrored; this is that same rule applied to the
        // figure that replaced it.
        final boolean flip = live && mirrorPreview;
        final java.util.function.DoubleUnaryOperator mapX =
                value -> flip ? offsetX + scale - value * scale : offsetX + value * scale;
        // The target pose, drawn faintly underneath and scaled to your own
        // body, so the correction is a shape to move into rather than a
        // sentence to decode. It is drawn before anything else and outside the
        // guards below, so it is still there on the frames where the camera has
        // lost you - which is exactly when you most need to know the shape.
        if (live) drawGuide(frame, offsetY, scale, mapX);
        var links = List.of(new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.RIGHT_SHOULDER}, new LandmarkName[]{LandmarkName.LEFT_HIP,LandmarkName.RIGHT_HIP}, new LandmarkName[]{LandmarkName.LEFT_HIP,LandmarkName.LEFT_KNEE}, new LandmarkName[]{LandmarkName.LEFT_KNEE,LandmarkName.LEFT_ANKLE}, new LandmarkName[]{LandmarkName.LEFT_ANKLE,LandmarkName.LEFT_TOE}, new LandmarkName[]{LandmarkName.RIGHT_HIP,LandmarkName.RIGHT_KNEE}, new LandmarkName[]{LandmarkName.RIGHT_KNEE,LandmarkName.RIGHT_ANKLE}, new LandmarkName[]{LandmarkName.RIGHT_ANKLE,LandmarkName.RIGHT_TOE}, new LandmarkName[]{LandmarkName.LEFT_SHOULDER,LandmarkName.LEFT_ELBOW}, new LandmarkName[]{LandmarkName.LEFT_ELBOW,LandmarkName.LEFT_WRIST}, new LandmarkName[]{LandmarkName.LEFT_WRIST,LandmarkName.LEFT_HAND}, new LandmarkName[]{LandmarkName.RIGHT_SHOULDER,LandmarkName.RIGHT_ELBOW}, new LandmarkName[]{LandmarkName.RIGHT_ELBOW,LandmarkName.RIGHT_WRIST}, new LandmarkName[]{LandmarkName.RIGHT_WRIST,LandmarkName.RIGHT_HAND});
        double limbWidth = Math.max(5, scale * .018);
        for (var link : links) {
            var a = frame.landmarks().get(link[0]); var b = frame.landmarks().get(link[1]);
            if (a == null || b == null) continue;
            double solidity = live ? visibility(a.confidence(), b.confidence()) : 1;
            if (solidity == 0) continue;
            var line = new Line(mapX.applyAsDouble(a.x()), offsetY+a.y()*scale, mapX.applyAsDouble(b.x()), offsetY+b.y()*scale);
            line.setStroke(LIMB); line.setStrokeWidth(limbWidth);
            line.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            line.setEffect(glow(LIMB.deriveColor(0, 1, 1, .55), limbWidth * 1.6));
            line.setOpacity(solidity);
            bodyView.getChildren().add(line);
        }
        var leftHip=frame.landmarks().get(LandmarkName.LEFT_HIP);var rightHip=frame.landmarks().get(LandmarkName.RIGHT_HIP);
        var leftShoulderPoint=frame.landmarks().get(LandmarkName.LEFT_SHOULDER);var rightShoulderPoint=frame.landmarks().get(LandmarkName.RIGHT_SHOULDER);
        // a live frame can be missing anything at all, including before the
        // first inference lands, so the torso is drawn only when it is there
        if (leftHip == null || rightHip == null || leftShoulderPoint == null || rightShoulderPoint == null) {
            if (live) return;
        }
        double spineStartX=mapX.applyAsDouble((leftShoulderPoint.x()+rightShoulderPoint.x())*.5),spineStartY=offsetY+(leftShoulderPoint.y()+rightShoulderPoint.y())*.5*scale;
        double spineEndX=mapX.applyAsDouble((leftHip.x()+rightHip.x())*.5),spineEndY=offsetY+(leftHip.y()+rightHip.y())*.5*scale;
        double sx=spineEndX-spineStartX,sy=spineEndY-spineStartY,length=Math.max(1,Math.hypot(sx,sy));
        double bend = live ? liveSpineBend(frame) : landmarks.spineBend();
        var spine=new QuadCurve(spineStartX,spineStartY,(spineStartX+spineEndX)/2-(sy/length)*bend*scale,(spineStartY+spineEndY)/2+(sx/length)*bend*scale,spineEndX,spineEndY);
        spine.setFill(Color.TRANSPARENT); spine.setStroke(LIMB); spine.setStrokeWidth(limbWidth * 1.15);
        spine.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        spine.setEffect(glow(LIMB.deriveColor(0, 1, 1, .55), limbWidth * 1.6));
        bodyView.getChildren().add(spine);
        frame.landmarks().entrySet().stream()
                .filter(entry -> entry.getKey() != LandmarkName.NOSE)
                .filter(entry -> !live || visibility(entry.getValue().confidence()) > 0)
                .forEach(entry -> {
            double radius = switch (entry.getKey()) { case LEFT_HAND,RIGHT_HAND,LEFT_TOE,RIGHT_TOE -> limbWidth * .62; default -> limbWidth * .92; };
            var p = entry.getValue();
            var dot = new Circle(mapX.applyAsDouble(p.x()), offsetY+p.y()*scale, radius, JOINT);
            dot.setEffect(glow(JOINT, radius * 2.6));
            if (live) dot.setOpacity(visibility(p.confidence()));
            bodyView.getChildren().add(dot);
        });
        var nose = frame.landmarks().get(LandmarkName.NOSE);
        var leftShoulder = frame.landmarks().get(LandmarkName.LEFT_SHOULDER);
        var rightShoulder = frame.landmarks().get(LandmarkName.RIGHT_SHOULDER);
        if (nose == null || leftShoulder == null || rightShoulder == null) return;
        // a head fades on the same terms as a limb: a face turned away scores
        // low, and a figure that loses its head is no better than a legless one
        double headSolidity = live ? visibility(nose.confidence()) : 1;
        if (headSolidity == 0) return;
        double headRadius = Math.max(20, scale*.052);
        double headCenterX = mapX.applyAsDouble(nose.x());
        double headCenterY = offsetY + nose.y()*scale;
        double headRadiusX=headRadius*.84, headRadiusY=headRadius*1.08;
        var head = new Ellipse(headCenterX, headCenterY, headRadiusX, headRadiusY);
        head.setFill(SKIN);
        head.setStroke(JOINT); head.setStrokeWidth(Math.max(4, scale * .014));
        head.setEffect(glow(JOINT, headRadius * 1.1));
        double shoulderX = mapX.applyAsDouble((leftShoulder.x()+rightShoulder.x())/2);
        double shoulderY = offsetY+((leftShoulder.y()+rightShoulder.y())/2)*scale;
        double dx=shoulderX-headCenterX,dy=shoulderY-headCenterY;
        double boundaryScale=1/Math.sqrt((dx*dx)/(headRadiusX*headRadiusX)+(dy*dy)/(headRadiusY*headRadiusY));
        var neck = new Line(headCenterX+dx*boundaryScale,headCenterY+dy*boundaryScale,shoulderX,shoulderY);
        neck.setStroke(LIMB); neck.setStrokeWidth(limbWidth);
        neck.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        neck.setOpacity(headSolidity); head.setOpacity(headSolidity);
        bodyView.getChildren().addAll(neck, head);
        int faceFrom = bodyView.getChildren().size();
        drawFace(headCenterX, headCenterY, headRadius, landmarks.faceDirection());
        if (headSolidity < 1) bodyView.getChildren().subList(faceFrom, bodyView.getChildren().size())
                .forEach(node -> node.setOpacity(headSolidity));
        var label = new Text(18, 28, live ? "Your body · nothing else from the room is drawn" : landmarks.description());
        label.setFill(Color.web("#b7c8c5"));
        bodyView.getChildren().add(label);
    }

    /**
     * Draws the pose you are aiming for, behind the body you actually have.
     *
     * The target comes from the same authored frame the analyzer measures
     * against, so the ghost cannot disagree with the cue you are being given.
     * It is fitted to you by a similarity transform: hips to hips, scaled by
     * torso length. That way it appears at your size and where you are standing
     * rather than floating at some canonical position, and matching it is the
     * instruction.
     *
     * When the camera cannot see your torso there is nothing to fit to, so the
     * target is drawn where it was authored - still useful, since a body that
     * is half out of frame needs the shape more than the alignment.
     */
    private void drawGuide(LandmarkFrame live, double offsetY, double scale, java.util.function.DoubleUnaryOperator mapX) {
        var target = demoSource.targetFrame().landmarks();
        var targetHip = midpointOf(target, LandmarkName.LEFT_HIP, LandmarkName.RIGHT_HIP);
        var targetShoulder = midpointOf(target, LandmarkName.LEFT_SHOULDER, LandmarkName.RIGHT_SHOULDER);
        if (targetHip == null || targetShoulder == null) return;
        double targetTorso = Math.hypot(targetShoulder[0] - targetHip[0], targetShoulder[1] - targetHip[1]);
        if (targetTorso < 1e-6) return;

        double fit = 1, shiftX = 0, shiftY = 0;
        var liveHip = confidentMidpoint(live, LandmarkName.LEFT_HIP, LandmarkName.RIGHT_HIP);
        var liveShoulder = confidentMidpoint(live, LandmarkName.LEFT_SHOULDER, LandmarkName.RIGHT_SHOULDER);
        if (liveHip != null && liveShoulder != null) {
            double liveTorso = Math.hypot(liveShoulder[0] - liveHip[0], liveShoulder[1] - liveHip[1]);
            if (liveTorso > 1e-6) {
                fit = liveTorso / targetTorso;
                shiftX = liveHip[0] - targetHip[0] * fit;
                shiftY = liveHip[1] - targetHip[1] * fit;
            }
        }
        final double k = fit, dx = shiftX, dy = shiftY;
        java.util.function.Function<Landmark, double[]> place = point -> new double[]{
                mapX.applyAsDouble(point.x() * k + dx), offsetY + (point.y() * k + dy) * scale};

        double width = Math.max(3, scale * .014);
        for (var link : GUIDE_LINKS) {
            var a = target.get(link[0]);
            var b = target.get(link[1]);
            if (a == null || b == null) continue;
            var from = place.apply(a);
            var to = place.apply(b);
            var line = new Line(from[0], from[1], to[0], to[1]);
            line.setStroke(GUIDE);
            line.setStrokeWidth(width);
            line.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            line.getStrokeDashArray().addAll(width * 2.4, width * 1.8);
            bodyView.getChildren().add(line);
        }
        var nose = target.get(LandmarkName.NOSE);
        if (nose != null) {
            var head = place.apply(nose);
            var circle = new Circle(head[0], head[1], Math.max(12, scale * .048 * k));
            circle.setFill(null);
            circle.setStroke(GUIDE);
            circle.setStrokeWidth(width);
            circle.getStrokeDashArray().addAll(width * 2.4, width * 1.8);
            bodyView.getChildren().add(circle);
        }
        // the ankles are where a pose is won or lost, so the target marks them
        // the same way your own joints are marked
        for (var ankle : List.of(LandmarkName.LEFT_ANKLE, LandmarkName.RIGHT_ANKLE)) {
            var point = target.get(ankle);
            if (point == null) continue;
            var at = place.apply(point);
            var mark = new Circle(at[0], at[1], Math.max(3, width * .9), JOINT.deriveColor(0, 1, 1, .55));
            mark.setEffect(glow(JOINT.deriveColor(0, 1, 1, .5), width * 2.2));
            bodyView.getChildren().add(mark);
        }
    }

    private double[] midpointOf(java.util.Map<LandmarkName, Landmark> points, LandmarkName left, LandmarkName right) {
        var a = points.get(left);
        var b = points.get(right);
        return a == null || b == null ? null : new double[]{(a.x() + b.x()) / 2, (a.y() + b.y()) / 2};
    }

    /** Only fits to joints the camera actually saw; a guessed hip would drag the guide off the body. */
    private double[] confidentMidpoint(LandmarkFrame frame, LandmarkName left, LandmarkName right) {
        var a = frame.landmarks().get(left);
        var b = frame.landmarks().get(right);
        if (a == null || b == null || a.confidence() < DRAW_THRESHOLD || b.confidence() < DRAW_THRESHOLD) return null;
        return new double[]{(a.x() + b.x()) / 2, (a.y() + b.y()) / 2};
    }

    /**
     * How far to bow the drawn spine for a live body.
     *
     * MoveNet has no mid-back keypoint, so the arch itself cannot be measured.
     * What it does give is the head, and in cat-cow the head leads the spine:
     * it tucks under as the back rounds and lifts as the back dips. Bowing the
     * curve toward whichever side of the shoulder-hip line the head has moved
     * to therefore follows the real movement, even though it is inferred from
     * the head rather than seen in the back.
     */
    private double liveSpineBend(LandmarkFrame frame) {
        var points = frame.landmarks();
        var nose = points.get(LandmarkName.NOSE);
        var leftShoulder = points.get(LandmarkName.LEFT_SHOULDER);
        var rightShoulder = points.get(LandmarkName.RIGHT_SHOULDER);
        var leftHip = points.get(LandmarkName.LEFT_HIP);
        var rightHip = points.get(LandmarkName.RIGHT_HIP);
        if (nose == null || leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) return 0;
        if (nose.confidence() < DRAW_THRESHOLD) return 0;
        double shoulderX = (leftShoulder.x() + rightShoulder.x()) / 2, shoulderY = (leftShoulder.y() + rightShoulder.y()) / 2;
        double hipX = (leftHip.x() + rightHip.x()) / 2, hipY = (leftHip.y() + rightHip.y()) / 2;
        double axisX = shoulderX - hipX, axisY = shoulderY - hipY;
        double axisLength = Math.hypot(axisX, axisY);
        if (axisLength < 1e-6) return 0;
        // signed perpendicular distance of the head from the spine's own axis,
        // measured in torso lengths so it does not change with distance
        double offX = nose.x() - shoulderX, offY = nose.y() - shoulderY;
        double sideways = (axisX * offY - axisY * offX) / axisLength / axisLength;
        return Math.max(-.09, Math.min(.09, sideways * .35));
    }

    /** A calm, friendly face: two eyes and a smile, turned the way the pose looks. */
    private void drawFace(double x, double y, double radius, FaceDirection direction) {
        double stroke = Math.max(2, radius * .11);
        if (direction == FaceDirection.FRONT || direction == FaceDirection.UP) {
            var left = eye(x - radius * .30, y - radius * .16, radius);
            var right = eye(x + radius * .30, y - radius * .16, radius);
            bodyView.getChildren().addAll(left, right, smile(x, y + radius * .10, radius * .46, stroke, 200, 140));
            return;
        }
        // side and floor views show the near eye only, with the smile swung
        // toward whatever the pose is looking at
        double vx = direction == FaceDirection.LEFT ? -1 : direction == FaceDirection.RIGHT ? 1 : 0;
        double vy = direction == FaceDirection.DOWN ? 1 : 0;
        var eye = eye(x + vx * radius * .26 - vy * radius * .12,
                y + vy * radius * .26 + Math.abs(vx) * radius * .10, radius);
        double startAngle = vx < 0 ? 250 : vx > 0 ? 110 : 200;
        bodyView.getChildren().addAll(eye,
                smile(x + vx * radius * .16, y + radius * .12 + vy * radius * .10, radius * .40, stroke, startAngle, 110));
    }

    /** Dark features on the lit head; the head's own glow is what carries the face. */
    private Circle eye(double x, double y, double headRadius) {
        return new Circle(x, y, Math.max(2, headRadius * .10), INK);
    }

    private javafx.scene.shape.Arc smile(double x, double y, double radius, double stroke, double start, double extent) {
        var arc = new javafx.scene.shape.Arc(x, y, radius, radius * .78, start, extent);
        arc.setType(javafx.scene.shape.ArcType.OPEN);
        arc.setFill(null);
        arc.setStroke(INK);
        arc.setStrokeWidth(stroke);
        arc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        return arc;
    }

    @Override public void stop() { if (clock != null) clock.stop(); stopCameraPreview(); voice.close(); landmarks.close(); }
    public static void main(String[] args) { launch(args); }
}
