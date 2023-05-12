package se.solrike.sonarlint.impl.util

import groovy.json.JsonBuilder
import io.github.furstenheim.CopyDown
import se.solrike.sonarlint.impl.IssueEx

/**
 * Generate SARIF JSON (https://sarifweb.azurewebsites.net) to be used when running in Github actions, Azure DevOps or AWS CodeCatalyst
 *
 *
 * @author lpersson
 *
 */
class SarifJsonBuilder {

  private static final Map<String, String> sIssueSeverityToLevel = ['BLOCKER':'error', 'CRITICAL':'error', 'MAJOR':'error', 'MINOR':'warning', 'INFO':'note']

  public SarifJsonBuilder() {
    super()
  }

  public Writer generateBugCollection(Writer writer, Collection<IssueEx> issues, File projectDir) {

    CopyDown markDownConverter = new CopyDown();

    // extract all unique rules from the issues
    Collection<IssueEx> ruleDescs = issues.toUnique { it.ruleKey }

    JsonBuilder builder = new groovy.json.JsonBuilder()

    builder {
      version '2.1.0'
      '$schema' 'https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0.json'
      runs ([
        {
          tool {
            driver {
              name 'Sonarlint'
              informationUri 'https://github.com/Lucas3oo/sonarlint-gradle-plugin'
              version '1.0.0'
              rules ruleDescs.collect { rule ->
                ({
                  id rule.ruleKey
                  helpUri 'https://rules.sonarsource.com'
                  defaultConfiguration (level:  sIssueSeverityToLevel[rule.severity])
                  shortDescription ( text: rule.message )
                  fullDescription {
                    text rule.message
                    markdown markDownConverter.convert(rule.rulesDetails.map({rd -> rd.htmlDescription}).orElse(rule.message))
                  }
                  properties  {
                    tags ([
                      rule.type
                    ])
                  }
                })
              }
            }
          }

          results issues.collect { issue ->
            ({
              ruleId issue.ruleKey
              level sIssueSeverityToLevel[issue.severity]
              message ( text: issue.message)
              locations ([
                {
                  physicalLocation {
                    artifactLocation {
                      uriBaseId "file://${projectDir.absolutePath}${File.separator}"
                      uri issue.inputFileRelativePath
                    }
                    region {
                      if (issue.startLine) {
                        startLine issue.startLine
                      }
                      if (issue.startLineOffset) {
                        startColumn issue.startLineOffset
                      }
                      if (issue.endLine) {
                        endLine issue.endLine
                      }
                      if (issue.endLineOffset) {
                        endColumn issue.endLineOffset
                      }
                    }
                  }
                }
              ])
            })
          }
        }
      ])
    }

    //println(builder.toPrettyString())

    builder.writeTo(writer)
    return writer
  }
}

// SARIF
/*
 *
 *
 "level": {
 "description": "A value specifying the severity level of the notification.",
 "default": "warning",
 "enum": [ "none", "note", "warning", "error" ]
 },
 *
 */

// Sonarlint:
// Rule key - ID for the pattern/issue. e.g java:s1234
// Severity - one of BLOCKER CRITICAL MAJOR MINOR INFO
// Type - one of BUG CODE_SMELL VULNERABILITY

