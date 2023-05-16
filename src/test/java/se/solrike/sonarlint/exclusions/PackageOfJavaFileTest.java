package se.solrike.sonarlint.exclusions;

import org.junit.jupiter.api.Test;

import java.io.File;
import static org.assertj.core.api.Assertions.*;

class PackageOfJavaFileTest {

    private final String exampleFileDirectory = "src/test/resources/examplejavafiles";
    private final File textFile = new File(exampleFileDirectory, "text.txt");
    private final File generatedFile = new File(exampleFileDirectory, "Generated.java");
    private final File noPackage = new File(exampleFileDirectory, "NoPackage.java");

    @Test
    void emptyWhenNotJava() {
        assertThat(PackageOfJavaFile.get(textFile)).isEmpty();
    }

    @Test
    void emptyWhenNoPackageFound() {
        assertThat(PackageOfJavaFile.get(noPackage)).isEmpty();
    }
    
    @Test
    void getPackageOfJavaFile() {
        assertThat(PackageOfJavaFile.get(generatedFile)).isNotEmpty().hasValue("com.company.generated");
    }
}