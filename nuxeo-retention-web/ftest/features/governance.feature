Feature: Retention Governance

  As a Record Cleaner, I delete a document under legal hold in governance mode

  Background:
    Given user "John" exists in group "RecordManager"
    And I have the following groups
      | name                    | label          |
      | NuxeoRecordCleaners     | RecordCleaners |
    And user "Jack" exists in group "NuxeoRecordCleaners"

  Scenario: Record Cleaner Role
    Given I have a File document
    And "John" has ManageLegalHold permission on the document
    When I login as "John"
    And I browse to the document
    Then I can see the "my document" document
    When I set a legal hold on the document with description "My legal hold"
    Then I see the document is under legal hold
    And I cannot see the trash button
    When I logout
    And I login as "Jack"
    And I have the following permissions to the documents
      | permission  | path            |
      | Everything  | /default-domain |
    And I browse to the document
    Then I can trash current document
    When I can navigate to trash pill
    # Adding this workaround because the Trash data-table is taking too long to load
    And I wait 3 seconds
    And I navigate to "my document" child
    Then I can permanently delete current document
