package net.swzo.brassworksupdater;

import net.neoforged.fml.loading.ImmediateWindowHandler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("^\\((\\d+/\\d+)\\)\\s+(.*)$");

    public static void runBrassworksUpdate() {
        try {
            Path tempDir = Files.createTempDirectory("bw-updater");
            Path updaterJar = tempDir.resolve("brassworks-updater-bootstrap.jar");

            boolean useDev = Boolean.parseBoolean(System.getProperty(DEV_PROPERTY, "false"));
            String updateUrl = useDev ? DEV_URL : System.getProperty(URL_PROPERTY, DEFAULT_URL);

            BrassworksUpdater.LOGGER.info("[BrassUpdater] Using URL: " + updateUrl);

            try (InputStream in = UpdaterBootstrap.class.getResourceAsStream("/updater/brassworks-updater-bootstrap.jar")) {
                if (in == null) throw new FileNotFoundException("Missing internal brassworks bootstrap jar!");
                Files.copy(in, updaterJar, StandardCopyOption.REPLACE_EXISTING);
            }
            BrassworksUpdater.LOGGER.info("[BrassUpdater] Bootstrapper extracted to " + updaterJar);

            ProcessBuilder pb = new ProcessBuilder("java", "-jar", updaterJar.toString(), updateUrl, "-g")
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

    private static void updateLoadingScreen(String rawLine) {
        String cleanLine = rawLine.replace("[BrassworksUpdater]", "").trim();
        if (cleanLine.isEmpty() || cleanLine.startsWith("UNSUPPORTED")) return;
        String displayMessage = "Updater: Working...";
        Matcher matcher = PROGRESS_PATTERN.matcher(cleanLine);
        if (matcher.find()) {
            String progress = matcher.group(1); 
            String content = matcher.group(2);

            if (content.contains("Downloaded")) {
                String modName = content.replace("Downloaded", "").trim();
                displayMessage = String.format("Downloading: %s (%s)", modName, progress);
            } else if (content.contains("already exists")) {
                String fileName = content.split(" ")[0];
                displayMessage = String.format("Verifying: %s (%s)", fileName, progress);
            } else {
                displayMessage = String.format("Processing: %s", progress);
            }
        } else {
            if (cleanLine.contains("Current version") || cleanLine.contains("New version")) {
                displayMessage = "Checking for updates...";
            } else if (cleanLine.contains("Loading manifest") || cleanLine.contains("Loading pack")) {
                displayMessage = "Loading configuration...";
            } else if (cleanLine.contains("Checking local files") || cleanLine.contains("Comparing new files") || cleanLine.contains("Validating")) {
                displayMessage = "Scanning local files...";
            } else if (cleanLine.contains("invalidated")) {
                displayMessage = "Found updates...";
            } else if (cleanLine.contains("Already up to date")) {
                displayMessage = "Pack is up to date!";
            } else if (cleanLine.contains("Finished successfully")) {
                displayMessage = "Update Complete!";
            } else {
                return;
            }
        }
        ImmediateWindowHandler.updateProgress(displayMessage);
    }
}