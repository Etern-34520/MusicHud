package indi.etern.musichud;

import dev.architectury.event.EventHandler;
import dev.architectury.platform.Platform;
import indi.etern.musichud.utils.ClassGraphRegistrationManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MusicHud {
    public static final String MOD_ID = "music_hud";
    public static final Random RANDOM = new Random();;
    private static final String LOGGER_BASE_NAME = "MusicHud";
    public static final Logger LOGGER = LogManager.getLogger(LOGGER_BASE_NAME);
    public static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    @Setter
    @Getter
    private static boolean connected = false;

    public static Logger getLogger(Class<?> clazz) {
        Logger logger = LogManager.getLogger(LOGGER_BASE_NAME + "/" + clazz.getSimpleName());
        logger.atLevel(Level.ALL);
        return logger;
    }

    public static void init() {
        LOGGER.atLevel(Level.ALL);
        EventHandler.init();
        ClassGraphRegistrationManager.performAutoRegistration(Platform.getEnv());
    }

    public static ResourceLocation location(String s) {
        return ResourceLocation.fromNamespaceAndPath(MusicHud.MOD_ID, s);
    }
}