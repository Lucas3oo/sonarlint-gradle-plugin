package se.solrike.sonarlint.exclusions;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class ExcludeByPackage implements Predicate<File> {

    private final Set<String> packages;
    private final Function<File, Optional<String>> getPackage;


    public ExcludeByPackage(Set<String> packages) {
        this(packages, PackageOfJavaFile::get);
    }

    public ExcludeByPackage(Set<String> packages, Function<File, Optional<String>> getPackage) {
        this.packages = packages;
        this.getPackage = getPackage;
    }

    @Override
    public boolean test(File file) {
        return packages.stream().noneMatch(p -> packageOfFileStartsWith(p, file));
    }

    private boolean packageOfFileStartsWith(String excludePackage, File file) {
        return getPackage.apply(file)
                .map(s -> s.startsWith(excludePackage))
                .orElse(false);
    }

}
