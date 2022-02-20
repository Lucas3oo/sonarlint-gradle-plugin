package se.solrike.sonarlint.impl;

import static java.util.Map.*;

import java.util.Map;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.sonarsource.sonarlint.core.commons.log.ClientLogOutput;

/**
 * @author Lucas Persson
 */
public class GradleClientLogOutput implements ClientLogOutput {

  private Logger mLogger;

  private static final Map<Level, LogLevel> sLevelMap = ofEntries(entry(Level.ERROR, LogLevel.ERROR),
      entry(Level.WARN, LogLevel.WARN), entry(Level.INFO, LogLevel.INFO), entry(Level.DEBUG, LogLevel.DEBUG),
      entry(Level.TRACE, LogLevel.DEBUG));

  public GradleClientLogOutput(Logger logger) {
    mLogger = logger;
  }

  @Override
  public void log(String formattedMessage, Level level) {
    if (!supress(formattedMessage)) {
      mLogger.log(sLevelMap.get(level), formattedMessage);
    }
  }

  private boolean supress(String formattedMessage) {
    return formattedMessage.startsWith("No workDir in SonarLint")
        || formattedMessage.startsWith("Analysis engine interrupted")
        || formattedMessage.startsWith("java.lang.InterruptedException");
  }

}
