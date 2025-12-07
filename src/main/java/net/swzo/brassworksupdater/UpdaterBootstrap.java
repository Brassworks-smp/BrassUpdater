package net.swzo.brassworksupdater;

import net.neoforged.fml.loading.ImmediateWindowHandler;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdaterBootstrap {
    private static final String URL_PROPERTY = "brassupdater.url";
    private static final String DEV_PROPERTY = "brassupdater.dev";
    private static final String DEFAULT_URL = "https://raw.githubusercontent.com/serverside-swzo/Brassworks-SMP-Season-2/master/pack.toml";
    private static final String DEV_URL = "https://raw.githubusercontent.com/serverside-swzo/Brassworks-SMP-Season-2/dev/pack.toml";

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("^\\((\\d+)/(\\d+)\\)\\s+(.*)$");

    private static ProgressMeter earlyProgress;
    private static Field stepsField;

    public static void runBrassworksUpdate() {
        try {
            Path tempDir = Files.createTempDirectory("bw-updater");
            Path updaterJar = tempDir.resolve("brassworks-updater-bootstrap.jar");
            String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            boolean useDev = Boolean.parseBoolean(System.getProperty(DEV_PROPERTY, "false"));
            String updateUrl = useDev ? DEV_URL : System.getProperty(URL_PROPERTY, DEFAULT_URL);

            var progressList = StartupNotificationManager.getCurrentProgress();
            if (!progressList.isEmpty()) {
                earlyProgress = progressList.get(0);
                setupReflection();
            }

            BrassworksUpdater.LOGGER.info("[BrassUpdater] Using URL: " + updateUrl);

            try (InputStream in = UpdaterBootstrap.class.getResourceAsStream("/updater/brassworks-updater-bootstrap.jar")) {
                if (in == null) throw new FileNotFoundException("Missing internal brassworks bootstrap jar!");
                Files.copy(in, updaterJar, StandardCopyOption.REPLACE_EXISTING);
            }
            BrassworksUpdater.LOGGER.info("[BrassUpdater] Bootstrapper extracted to " + updaterJar);

            ProcessBuilder pb = new ProcessBuilder(javaBin, "-jar", updaterJar.toString(), updateUrl, "-g")
                    .redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[BrassworksUpdater] " + line);
                    updateLoadingScreen(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                BrassworksUpdater.LOGGER.error("[BrassUpdater] Exited with code " + exitCode);
            } else {
                BrassworksUpdater.LOGGER.info("[BrassUpdater] Update complete.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to run BrassUpdater", e);
        }
    }

    private static void setupReflection() {
        try {

            stepsField = ProgressMeter.class.getDeclaredField("steps");
            stepsField.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[BrassUpdater] Failed to set up reflection for progress bar: " + e.getMessage());
        }
    }

    private static void updateLoadingScreen(String rawLine) {
        String cleanLine = rawLine.replace("[BrassworksUpdater]", "").trim();
        if (cleanLine.isEmpty() || cleanLine.startsWith("UNSUPPORTED")) return;

        String displayMessage = "Updater: Working...";
        Matcher matcher = PROGRESS_PATTERN.matcher(cleanLine);

        if (matcher.find()) {

            int current = 0;
            int total = 0;
            try {
                current = Integer.parseInt(matcher.group(1));
                total = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException ignored) {}

            String content = matcher.group(3);
            String progressStr = matcher.group(1) + "/" + matcher.group(2);

            if (content.contains("Downloaded")) {
                String modName = content.replace("Downloaded", "").trim();
                displayMessage = String.format("Downloading: %s (%s)", modName, progressStr);
            } else if (content.contains("already exists")) {
                String fileName = content.split(" ")[0];
                displayMessage = String.format("Verifying: %s (%s)", fileName, progressStr);
            } else {
                displayMessage = String.format("Processing: %s", progressStr);
            }

            setProgressBarState(current, total);

        } else {

            boolean isIndeterminate = true;

            if (cleanLine.contains("Current version") || cleanLine.contains("New version")) {
                displayMessage = "Checking for updates...";
            } else if (cleanLine.contains("Loading manifest") || cleanLine.contains("Loading pack")) {
                displayMessage = "Loading configuration...";
            } else if (cleanLine.contains("Checking local files") || cleanLine.contains("Comparing new files")) {
                displayMessage = "Scanning local files...";
            } else if (cleanLine.contains("invalidated")) {
                displayMessage = "Found updates...";
            } else if (cleanLine.contains("Already up to date")) {
                displayMessage = "Pack is up to date!";
                isIndeterminate = false; 
            } else if (cleanLine.contains("Finished successfully")) {
                displayMessage = "Update Complete!";
                isIndeterminate = false; 
            } else {
                return; 
            }

            if (isIndeterminate) {
                setIndeterminate();
            } else {
                setProgressBarState(1, 1); 
            }
        }

        ImmediateWindowHandler.updateProgress(displayMessage);
    }

    private static void setProgressBarState(int current, int total) {
        if (earlyProgress != null && stepsField != null && total > 0) {
            try {

                if (earlyProgress.steps() != total) {
                    stepsField.setInt(earlyProgress, total);
                }
                earlyProgress.setAbsolute(current);
            } catch (Exception e) {

            }
        }
    }

    private static void setIndeterminate() {
        if (earlyProgress != null && stepsField != null) {
            try {

                if (earlyProgress.steps() != 0) {
                    stepsField.setInt(earlyProgress, 0); 
                    earlyProgress.setAbsolute(0);
                }
            } catch (Exception e) {

            }
        }
    }
}