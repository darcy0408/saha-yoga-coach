plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "io.saha"
version = "0.1.0"
val targetJavaVersion = providers.gradleProperty("javaVersion").map(String::toInt).orElse(26)

repositories { mavenCentral() }

java {
    // Java 26 is the product target. The property exists only so contributors can
    // run structural checks on older hosts while waiting for a JDK 26 install.
    toolchain { languageVersion = JavaLanguageVersion.of(targetJavaVersion.get()) }
}

javafx {
    version = if (targetJavaVersion.get() >= 26) "26" else "21.0.8"
    modules = listOf("javafx.controls", "javafx.graphics")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
    implementation("com.microsoft.onnxruntime:onnxruntime:1.22.0")
    implementation("org.openpnp:opencv:4.9.0-0")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "io.saha.yoga.SahaApp"
    applicationDefaultJvmArgs = listOf("--enable-preview", "--enable-native-access=javafx.graphics,ALL-UNNAMED")
}

tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("--enable-preview") }
tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.register<JavaExec>("figureSnapshot") {
    group = "verification"
    description = "Renders the observed-landmarks figure to build/review/figure.png"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.saha.yoga.illustration.FigureSnapshotLauncher"
    args(layout.buildDirectory.file("review/figure.png").get().asFile.absolutePath)
    jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED")
}

tasks.register<JavaExec>("colourCheck") {
    group = "verification"
    description = "Prints how the preview pipeline interprets known pixel bytes"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.saha.yoga.vision.ColourCheckLauncher"
    jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED")
}

tasks.register<JavaExec>("cameraCheck") {
    group = "verification"
    description = "Reports which camera devices and backends this machine can actually open and read"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.saha.yoga.vision.CameraCheckLauncher"
    jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED")
    // gradlew cameraCheck --args="--all" to probe every backend, at the risk
    // of leaving a device wedged by a Media Foundation open that never returns
    if (project.hasProperty("allBackends")) args("--all")
}

tasks.register<JavaExec>("chimeCheck") {
    group = "verification"
    description = "Plays the pose-came-right chime, and reports whether this machine can play it"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.saha.yoga.sound.ChimeCheckLauncher"
    jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED")
}

tasks.register<JavaExec>("routineSnapshot") {
    group = "verification"
    description = "Renders the generated practice in order to build/review/routine.png"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.saha.yoga.illustration.RoutineSnapshotLauncher"
    args(layout.buildDirectory.file("review/routine.png").get().asFile.absolutePath)
    jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED")
}

tasks.register<JavaExec>("iconGallerySnapshot") {
    group = "verification"
    description = "Renders every Atlas pose icon to build/review/icon-gallery.png"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.saha.yoga.illustration.IconGallerySnapshotLauncher"
    args(layout.buildDirectory.file("review/icon-gallery.png").get().asFile.absolutePath)
    jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED")
}

tasks.register<JavaExec>("teachingCardSnapshot") {
    group = "verification"
    description = "Renders the coaching teaching card, illustrated and written-only, to build/review/teaching-card.png"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.saha.yoga.illustration.TeachingCardSnapshotLauncher"
    args(layout.buildDirectory.file("review/teaching-card.png").get().asFile.absolutePath)
    jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED")
}

tasks.register<JavaExec>("poseGallerySnapshot") {
    group = "verification"
    description = "Renders licensed teaching-asset candidates to build/review/pose-gallery.png"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.saha.yoga.illustration.PoseGallerySnapshotLauncher"
    args(layout.buildDirectory.file("review/pose-gallery.png").get().asFile.absolutePath)
    jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED")
}
