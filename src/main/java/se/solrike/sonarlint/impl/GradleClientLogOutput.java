package se.solrike.sonarlint.impl;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

import java.util.Map;

import javax.annotation.Nullable;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.sonarsource.sonarlint.core.commons.log.LogOutput;

/**
 * @author Lucas Persson
 */
public class GradleClientLogOutput implements LogOutput {

  private final Logger mLogger;

  private static final Map<Level, LogLevel> sLevelMap = ofEntries(entry(Level.ERROR, LogLevel.ERROR),
      entry(Level.WARN, LogLevel.WARN), entry(Level.INFO, LogLevel.INFO), entry(Level.DEBUG, LogLevel.DEBUG),
      entry(Level.TRACE, LogLevel.DEBUG));

  public GradleClientLogOutput(Logger logger) {
    mLogger = logger;
  }

  @Override
  public void log(@Nullable String formattedMessage, Level level, @Nullable String stacktrace) {
    if (formattedMessage != null && !supress(formattedMessage)) {
      mLogger.log(sLevelMap.get(level), formattedMessage);
    }
    if (stacktrace != null ) {
      mLogger.log(sLevelMap.get(level), stacktrace);
    }
  }


  private boolean supress(String formattedMessage) {
    return formattedMessage.startsWith("No workDir in SonarLint")
        || formattedMessage.startsWith("Analysis engine interrupted")
        || formattedMessage.startsWith("com.google.gson.JsonIOException")
        || formattedMessage.startsWith("java.lang.InterruptedException");
  }

}
