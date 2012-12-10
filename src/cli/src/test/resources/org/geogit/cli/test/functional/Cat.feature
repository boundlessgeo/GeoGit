Feature: "cat" command
    In order to know the content of a given element
    As a Geogit User
    I want to display its content

  Scenario: Try to show the content of a tree.
    Given I have a repository
      And I stage 6 features
     When I run the command "commit -m TestCommit"
     When I run the command "cat HEAD:Points"
     Then the response should contain "Points.1"
      And the response should contain "Points.2"
      And the response should contain "Points.3"
     
Scenario: Try to show the content of a feature.
    Given I have a repository
      And I stage 6 features
     When I run the command "commit -m TestCommit"
     When I run the command "cat HEAD:Points/Points.1"
     Then the response should contain "1000"
      And the response should contain "POINT (1 1)"
      And the response should contain "StringProp1_1"
     
Scenario: Try to show the content of HEAD.
    Given I have a repository
      And I stage 6 features
     When I run the command "commit -m TestCommit"
     When I run the command "cat HEAD"
     Then the response should contain "Test"
      And the response should contain "RevPerson"
      And the response should contain "Points"