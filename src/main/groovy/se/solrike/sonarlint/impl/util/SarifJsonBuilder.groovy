package se.solrike.sonarlint.impl.util

import static java.util.Map.entry
import static java.util.Map.ofEntries

import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails
import org.sonarsource.sonarlint.core.commons.IssueSeverity

import groovy.json.JsonBuilder
import io.github.furstenheim.CodeBlockStyle
import io.github.furstenheim.CopyDown
import io.github.furstenheim.OptionsBuilder
import se.solrike.sonarlint.impl.IssueEx

/**
 * Generate SARIF JSON (https://sarifweb.azurewebsites.net) to be used when running in Github actions,
 * Azure DevOps or AWS CodeCatalyst
 *
 *
 * @author lpersson
 *
 */
class SarifJsonBuilder {

  // @formatter:off
  //CHECKSTYLE:OFF
  private static final Map<IssueSeverity, String> sIssueSeverityToLevel  = ofEntries(
      entry(IssueSeverity.BLOCKER,  'error'),
      entry(IssueSeverity.CRITICAL, 'error'),
      entry(IssueSeverity.MAJOR,    'error'),
      entry(IssueSeverity.MINOR,    'warning'),
      entry(IssueSeverity.INFO,     'note')
      );
  // @formatter:on
  // CHECKSTYLE:ON


  public SarifJsonBuilder() {
    super()
  }

  public Writer generateBugCollection(Writer writer, Collection<IssueEx> issues, File projectDir) {
    CopyDown markDownConverter = new CopyDown(OptionsBuilder.anOptions().withCodeBlockStyle(CodeBlockStyle.FENCED).build());

    // extract all unique rules from the issues
    Collection<IssueEx> ruleDescs = issues.toUnique { it.ruleKey }

    JsonBuilder builder = new groovy.json.JsonBuilder()

    builder {
      '$schema' 'https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0.json'
      version '2.1.0'
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
                  helpUri this.buildHelpUri(rule.ruleKey, rule.rulesDetails)
                  defaultConfiguration (level:  sIssueSeverityToLevel[rule.severity])
                  shortDescription ( text: rule.message )
                  fullDescription ( text: rule.message )
                  help {
                    text ''
                    markdown markDownConverter.convert(rule.rulesDetails.map({rd -> fixPreCode(rd.htmlDescription)}).orElse(rule.message))
                  }
                  List theTags = [rule.type]
                  theTags.addAll(rule.rulesDetails.map({rd -> rd.tags}).orElse([]))
                  properties  {
                    tags ( theTags )
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


  String fixPreCode(String html) {
    return html.replace("<pre>", "<pre><code>").replace("</pre>", "</pre></code>")
  }

  String buildHelpUri(String ruleKey, Optional<StandaloneRuleDetails> ruleDetails ) {
    if (keyHasId(ruleKey)) {
      String id = getKeyId(ruleKey);
      return 'https://rules.sonarsource.com' + ruleDetails.map({ rd ->
        '/' + rd.language + '/' + 'RSPEC-' + id
      }
      ).orElse('')
    }
    else {
      return 'https://rules.sonarsource.com'
    }
  }

  // if the key is like java:S1176 then it has a numeric "id" in it that we can sort on.
  boolean keyHasId(String key) {
    return key.matches(".*:S[0-9]+");
  }

  // must be on form similar to java:S1176
  int getKeyId(String key) {
    String id = key.substring(key.indexOf(":S") + 2);
    return Integer.parseInt(id);
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

