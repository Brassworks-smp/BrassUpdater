package net.swzo.brassworksupdater;

import cpw.mods.modlauncher.api.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UpdatePlugin implements ITransformationService {

    @Override
    public @NotNull String name() {
        return "brassworks_updater";
    }

    @Override
    public void initialize(IEnvironment environment) {
        if (Boolean.getBoolean("brassupdater.skip")) {
            BrassworksUpdater.LOGGER.info("[BrassUpdater] Skipping update check because 'brassupdater.skip' is set to true.");
            return;
        }
        BrassworksUpdater.LOGGER.info("[BrassUpdater] Service initializing. Running Brassworks updater...");
        try {
            UpdaterBootstrap.runBrassworksUpdate();
        } catch (Exception e) {
            BrassworksUpdater.LOGGER.error("[BrassUpdater] Update failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) throws IncompatibleEnvironmentException {}

    @Override
    public @NotNull List<ITransformer<?>> transformers() {
        return new ArrayList<>();
    }

    @Override
    public List<Resource> beginScanning(IEnvironment environment) {
        return List.of();
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        return List.of();
    }
}