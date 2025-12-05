package net.swzo.brassworksupdater;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class UpdaterBootstrap {

    public static void runBrassworksUpdate() {
        try {
            Path temp = Files.createTempDirectory("bw-updater");
            Path updater = temp.resolve("brassworks-updater-bootstrap.jar");
            String URL = "https://raw.githubusercontent.com/serverside-swzo/Brassworks-SMP-Season-2/master/pack.toml";
            try (InputStream in = UpdaterBootstrap.class.getResourceAsStream("/updater/brassworks-updater-bootstrap.jar")) {
                if (in == null) throw new FileNotFoundException("Missing internal brassworks bootstrap jar! Make sure it is in src/main/resources/updater/");
                Files.copy(in, updater, StandardCopyOption.REPLACE_EXISTING);
            }
            BrassworksUpdater.LOGGER.info("[BrassUpdater] Brassworks bootstrapper extracted to " + updater);
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", updater.toString(), URL, "-g");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) System.out.println("[BrassworksUpdater] " + line);
            }
            int exitCode = proc.waitFor();
            if (exitCode != 0) BrassworksUpdater.LOGGER.error("[BrassUpdater] BrassworksUpdater exited with code " + exitCode);
            else BrassworksUpdater.LOGGER.info("[BrassUpdater] Update complete.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to run BrassUpdater", e);
        }
    }
}