package se.solrike.sonarlint.exclusions;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
class ExcludeByPackageTest {

    private static final Map<File, String> testFiles = new HashMap<>();
    private static final Function<File, Optional<String>> getPackageMock = file ->
            Optional.ofNullable(testFiles.get(file));

    private static final File NORMALFILE = new File("somefile.java");
    private static final File GENERATED_FILE = new File("somefilegenerated.java");
    private static final File GENERATED_FILE_SUB_PACKAGE = new File("somefilegeneratedinpackage.java");

    static {
        testFiles.put(NORMALFILE, "com.company.production.something");
        testFiles.put(GENERATED_FILE, "com.company.generated");
        testFiles.put(GENERATED_FILE_SUB_PACKAGE, "com.company.generated.nested");
    }

    @Test
    void generatedShouldBeExcluded() {
        ExcludeByPackage excludeGenerated = new ExcludeByPackage(setOf("com.company.generated"), getPackageMock);
        assertThat(excludeGenerated.test(NORMALFILE)).isTrue();
        assertThat(excludeGenerated.test(GENERATED_FILE)).isFalse();
        assertThat(excludeGenerated.test(GENERATED_FILE_SUB_PACKAGE)).isTrue();
    }

    @Test
    void generatedShouldNotBeExcluded() {
        ExcludeByPackage excludeGenerated = new ExcludeByPackage(setOf(), getPackageMock);
        assertThat(excludeGenerated.test(NORMALFILE)).isTrue();
        assertThat(excludeGenerated.test(GENERATED_FILE)).isTrue();
        assertThat(excludeGenerated.test(GENERATED_FILE_SUB_PACKAGE)).isTrue();
    }

    private static Set<String> setOf(String... packages) {
        Set<String> setOfPackages = new HashSet<>();
        Collections.addAll(setOfPackages, packages);
        return setOfPackages;
    }
}