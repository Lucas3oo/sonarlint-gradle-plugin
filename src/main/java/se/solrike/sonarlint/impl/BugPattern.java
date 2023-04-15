package se.solrike.sonarlint.impl;

import java.util.Objects;

/**
 * Sportbugs BugPatter item in the XML report
 */
public class BugPattern {

  private String mType;
  private String mCategory;
  private String mShortDescription;
  private String mDetails;

  public BugPattern(String type, String category, String shortDescription, String details) {
    super();
    mType = type;
    mCategory = category;
    mShortDescription = shortDescription;
    mDetails = details;
  }

  public String getType() {
    return mType;
  }

  public void setType(String type) {
    mType = type;
  }

  public String getCategory() {
    return mCategory;
  }

  public void setCategory(String category) {
    mCategory = category;
  }

  public String getShortDescription() {
    return mShortDescription;
  }

  public void setShortDescription(String shortDescription) {
    mShortDescription = shortDescription;
  }

  public String getDetails() {
    return mDetails;
  }

  public void setDetails(String details) {
    mDetails = details;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mType);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    BugPattern other = (BugPattern) obj;
    return Objects.equals(mType, other.mType);
  }

}
