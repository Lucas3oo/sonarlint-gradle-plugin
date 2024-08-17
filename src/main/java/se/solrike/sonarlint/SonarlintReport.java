package se.solrike.sonarlint;

import org.gradle.api.Named;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

/**
 * The reporting configuration for the {@link Sonarlint} task.
 * <p>
 * One of
 * <ul>
 * <li>html
 * <li>sarif
 * <li>text
 * <li>xml
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
 *         sarif.enabled = true // default false
 *         xml.enabled = true // default false
 *     }
 * }
 * </pre>
 *
 * @see Sonarlint
 *
 * @author Lucas Persson
 */
//https://docs.gradle.org/8.6/userguide/custom_gradle_types.html#nameddomainobjectset

public interface SonarlintReport extends Named {

  @Input
  Property<Boolean> getEnabled();

  @Input
  RegularFileProperty getOutputLocation();

}
