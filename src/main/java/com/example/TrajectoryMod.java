package com.dmitrofnet.trajectory;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrajectoryMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("trajectory");

    @Override
    public void onInitializeClient() {
        // Mixins handle the heavy lifting since Events are gone in 1.21.10
        LOGGER.info("Trajectory Mod initialized");
    }
}