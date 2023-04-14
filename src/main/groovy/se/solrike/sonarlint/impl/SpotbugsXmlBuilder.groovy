package se.solrike.sonarlint.impl

import org.apache.commons.text.StringEscapeUtils

import groovy.xml.MarkupBuilder

class SpotbugsXmlBuilder {

  public SpotbugsXmlBuilder() {
    super()
  }

  public Writer generateIssues(Writer writer, Collection<IssueEx> issues) {
    MarkupBuilder bugCollection = new MarkupBuilder(writer)
    bugCollection.mkp.xmlDeclaration([version:'1.0',encoding:'UTF-8'])
    bugCollection.BugCollection {
      issues.each { issue ->
        bugCollection.BugInstance (type: issue.ruleKey, priority: 1, rank: getIssueSeverity(issue.severity), category: issue.type) {
          ShortMessage (StringEscapeUtils.escapeHtml4(issue.getMessage()))
          if (issue.rulesDetails.isPresent()) {
            LongMessage ('<![CDATA[' + issue.rulesDetails.get().getHtmlDescription() + ']]>')
          }
          SourceLine (classname: '', start: issue.getStartLine(), end: issue.getEndLine(),
          sourcefile: issue.getFileName(), sourcepath: issue.getInputFileRelativePath(),
          relSourcepath: issue.getInputFileRelativePath(), synthetic: 'true')
        }
      }
    }

    return writer
  }

  private static final Map<String, Integer> sIssueSeverity = ['BLOCKER':1, 'CRITICAL':3, 'MAJOR':5, 'MINOR':7, 'INFO':20]

  public Integer getIssueSeverity(String issueSeverity) {
    return sIssueSeverity.get(issueSeverity);
  }
}



// Type - This is just the name of the BugPattern which was found.
// Rank - indicates the severity 20-1 (1 of highest severity)
// Priority - It indicates the confidence that the found bug is actually a bug.  It varies from 1 (highest confidence) to 5 (lowest confidence, to be disregarded)
// Category - The category is for grouping BugPatterns, e.g.  CORRECTNESS

/*
 <BugInstance type="FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY" priority="2" rank="9" abbrev="FCCD" category="CORRECTNESS" instanceHash="eb77c6533bddc4608aee82dd6d0b920d" instanceOccurrenceNum="0" instanceOccurrenceMax="0">
 <ShortMessage>Class has a circular dependency with other classes</ShortMessage>
 <LongMessage>Class se.solrike.sonarlint.Sonarlint has a circular dependency with other classes</LongMessage>
 <SourceLine classname="se.solrike.sonarlint.Sonarlint" start="36" end="194" sourcefile="Sonarlint.java" sourcepath="se/solrike/sonarlint/Sonarlint.java" relSourcepath="java/se/solrike/sonarlint/Sonarlint.java" synthetic="true">
 <Message>At Sonarlint.java:[lines 36-194]</Message>
 </SourceLine>
 */
