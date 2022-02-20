package se.solrike.sonarlint.impl;

import org.gradle.api.logging.Logger;
import org.sonarsource.sonarlint.core.commons.progress.ClientProgressMonitor;

/**
 * @author Lucas Persson
 */
public class GradleProgressMonitor implements ClientProgressMonitor {

  private Logger mLogger;
  private float mFraction;
  private boolean mIndeterminate;

  public GradleProgressMonitor(Logger logger) {
    mLogger = logger;
  }

  @Override
  public void setMessage(String msg) {
    if (mIndeterminate) {
      mLogger.info("Fraction: {}. {}", mFraction, msg);
    }
    else {
      mLogger.info(msg);
    }
  }

  @Override
  public void setFraction(float fraction) {
    mFraction = fraction;
  }

  @Override
  public void setIndeterminate(boolean indeterminate) {
    mIndeterminate = indeterminate;
  }

}
