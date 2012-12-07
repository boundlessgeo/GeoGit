Feature: "cat" command
    In order to know the content of a given element
    As a Geogit User
    I want to display its content

  Scenario: Try to show the content of a tree.
    Given I have a repository
      And I stage 6 features
     When I run the command "commit -m TestCommit"
     When I run the command "cat Points"
     Then the response should contain "1000"
     
Scenario: Try to show the content of a feature.
    Given I have a repository
      And I stage 6 features
     When I run the command "commit -m TestCommit"
     When I run the command "cat Points/Points.1"
     Then the response should contain "Subject: TestCommit"
     
Scenario: Try to show the content of a tree.
    Given I have a repository
      And I stage 6 features
     When I run the command "commit -m TestCommit"
     When I run the command "cat HEAD"
     Then the response should contain "Subject: TestCommit"