package se.solrike.sonarlint.exclusions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PackageOfJavaFile {

    private static final Pattern PACKAGE_REGEX = Pattern.compile("^package (.+?);$");

    private PackageOfJavaFile(){
        // static use only
    }

    public static Optional<String> get(File file) {
        if(!file.getName().endsWith(".java")){
            return Optional.empty();
        }
        try {
            return parsePackage(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<String> parsePackage(File file) throws IOException {
        try(Stream<String> lines = Files.lines(file.toPath())){
            return lines
                    .filter(PackageOfJavaFile::isPackageDeclarationLine)
                    .findFirst()
                    .map(PackageOfJavaFile::extractPackage);
        }
    }

    private static boolean isPackageDeclarationLine(String line) {
        return line.startsWith("package ");
    }

    private static String extractPackage(String line) {
        Matcher matcher = PACKAGE_REGEX.matcher(line);
        if(matcher.matches()){
            return matcher.group(1);
        }
        throw new IllegalArgumentException(String.format("could not read package from line: %s", line));
    }
}
