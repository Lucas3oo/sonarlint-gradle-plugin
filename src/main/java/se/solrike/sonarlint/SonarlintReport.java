package se.solrike.sonarlint;

import javax.inject.Inject;

import org.gradle.api.Named;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * The reporting configuration for the {@link Sonarlint} task.
 * <p>
 * One of
 * <ul>
 * <li>html
 * <li>text
 * </ul>
 * <p>
 * The reporting aspects of a {@link Sonarlint} task can be configured as such:
 *
 * <pre>
 * sonarlint {
 *     reports {
 *         html.enabled = false // default false
 *         text {
 *             enabled = true // default false
 *             // override default report file location for text report
 *             outputLocation = layout.buildDirectory.file('my_sonarlint.txt')
 *         }
 *     }
 * }
 * </pre>
 *
 * @see Sonarlint
 *
 * @author Lucas Persson
 */
public abstract class SonarlintReport implements Named {
  private final String mName;

  @SuppressWarnings("all")
  @Inject
  public SonarlintReport(String name) {
    mName = name;
  }

  @Override
  public String getName() {
    return mName;
  }

  public abstract Property<Boolean> getEnabled();

  public abstract RegularFileProperty getOutputLocation();

  @Override
  public String toString() {
    return getEnabled().getOrElse(false) + ", location: " + getOutputLocation().getOrNull();
  }

}
