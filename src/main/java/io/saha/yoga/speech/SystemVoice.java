package io.saha.yoga.speech;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Speaks through the operating system's own voice, on this machine.
 *
 * <p>No audio and no text leaves the process: on Windows this drives the
 * built-in .NET speech synthesiser, which is the same engine Narrator uses.
 *
 * <p>One long-lived helper process is kept open and fed a line at a time,
 * because starting a new one per sentence costs about a second — long enough
 * that a cue would arrive after the moment it described. The helper prints a
 * marker when it finishes speaking, which is how this class knows the voice is
 * free again.
 *
 * <p>Only the most recent pending line is kept. During a practice a stale cue
 * is worse than no cue, so a queue that could run behind is deliberately not
 * offered.
 */
public final class SystemVoice implements Voice {
    private static final String SCRIPT = """
            Add-Type -AssemblyName System.Speech
            $voice = New-Object System.Speech.Synthesis.SpeechSynthesizer
            $voice.Rate = -1
            while ($true) {
              $line = [Console]::In.ReadLine()
              if ($null -eq $line) { break }
              if ($line.Trim().Length -gt 0) { $voice.Speak($line) }
              [Console]::Out.WriteLine('.')
              [Console]::Out.Flush()
            }
            """;

    private final Process process;
    private final BufferedWriter toVoice;
    private final AtomicReference<String> pending = new AtomicReference<>();
    private final Object idle = new Object();
    private volatile boolean ready = true;
    private volatile boolean running = true;

    private SystemVoice(Process process) {
        this.process = process;
        this.toVoice = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        Thread.ofVirtual().name("saha-voice-reader").start(this::readMarkers);
        Thread.ofVirtual().name("saha-voice-writer").start(this::writeLines);
    }

    /** Falls back to silence when this platform has no voice this class can drive. */
    public static Voice create() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return Voice.silent();
        try {
            // -EncodedCommand takes UTF-16LE base64, which sidesteps Windows
            // command-line quoting entirely: the script contains quotes of its
            // own, and passing it as plain text lets the shell mangle them.
            var encoded = Base64.getEncoder().encodeToString(SCRIPT.getBytes(StandardCharsets.UTF_16LE));
            var builder = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", encoded);
            builder.redirectErrorStream(true);
            return new SystemVoice(builder.start());
        } catch (Exception e) {
            return Voice.silent();
        }
    }

    @Override public void say(String text) {
        if (text == null || text.isBlank()) return;
        pending.set(text.replaceAll("\\s+", " ").trim());
        synchronized (idle) { idle.notifyAll(); }
    }

    @Override public boolean isAvailable() { return running && process.isAlive(); }

    private void readMarkers() {
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            while (running && reader.readLine() != null) {
                ready = true;
                synchronized (idle) { idle.notifyAll(); }
            }
        } catch (Exception ignored) {
            // the helper died; say() becomes a no-op from here
        } finally {
            running = false;
        }
    }

    private void writeLines() {
        while (running) {
            String next = null;
            synchronized (idle) {
                if (!ready || pending.get() == null) {
                    try {
                        idle.wait(250);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (ready) next = pending.getAndSet(null);
            }
            if (next == null) continue;
            try {
                ready = false;
                toVoice.write(next);
                toVoice.newLine();
                toVoice.flush();
            } catch (Exception e) {
                running = false;
            }
        }
    }

    @Override public void close() {
        running = false;
        try {
            toVoice.close();
        } catch (Exception ignored) {
            // shutting down; the process is destroyed next regardless
        }
        process.destroy();
    }
}
