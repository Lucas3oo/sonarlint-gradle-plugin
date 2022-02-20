package se.solrike.sonarlint.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;

/**
 * @author Lucas Persson
 */
public class ClientInputFileImpl implements ClientInputFile {

  private final Path mPath;
  private final boolean mTest;
  private final Charset mCharset;
  private final Path mBaseDir;

  public ClientInputFileImpl(Path baseDir, Path path, boolean test, Charset charset) {
    mBaseDir = baseDir;
    mPath = path;
    mTest = test;
    mCharset = charset;
  }

  @SuppressWarnings("all")
  @Override
  @Deprecated
  public String getPath() {
    return mPath.toString();
  }

  /**
   * Flag an input file as test file. Analyzers may apply different rules on test files.
   */
  @Override
  public boolean isTest() {
    return mTest;
  }

  @Override
  public Charset getCharset() {
    return mCharset;
  }

  @Override
  public <G> G getClientObject() {
    return null;
  }

  @Override
  public InputStream inputStream() throws IOException {
    return Files.newInputStream(mPath);
  }

  @Override
  public String contents() throws IOException {
    return new String(Files.readAllBytes(mPath), mCharset);
  }

  @Override
  public String relativePath() {
    return mBaseDir.relativize(mPath).toString();
  }

  @Override
  public URI uri() {
    return mPath.toUri();
  }

  @Override
  public String toString() {
    return relativePath();
  }

}
