package mod.chiselsandbits.core;

import mod.chiselsandbits.helpers.ExceptionNoTileEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {

    private Log() {}

    private static Logger getLogger() {
        return LogManager.getLogger(ChiselsAndBits.MODID);
    }

    public static void logError(final String message, final Throwable e) {
        getLogger().error(message, e);
    }

    public static void info(final String message) {
        getLogger().info(message);
    }

    public static void noTileError(final ExceptionNoTileEntity e) {
        if (ChiselsAndBits.getConfig().getServer().logTileErrors.get()) {
            getLogger().error("Unable to find TileEntity while interacting with block.", new Exception());
        }
    }

    public static void eligibility(String message) {
        if (ChiselsAndBits.getConfig().getServer().logEligibilityErrors.get()) {
            getLogger().info(message);
        }
    }
}
