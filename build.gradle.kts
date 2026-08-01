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
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics")
}

tasks.test { useJUnitPlatform() }

tasks.register<JavaExec>("poseGallerySnapshot") {
    group = "verification"
    description = "Renders the grounded teaching pose gallery to build/review/pose-gallery.png"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "io.saha.yoga.illustration.PoseGallerySnapshotLauncher"
    args(layout.buildDirectory.file("review/pose-gallery.png").get().asFile.absolutePath)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
