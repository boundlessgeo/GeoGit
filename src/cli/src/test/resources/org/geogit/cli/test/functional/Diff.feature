Feature: "diff" command
    In order to know changes made in a repository
    As a Geogit User
    I want to see the existing differences between commits 
     
Scenario: Show diff between working tree and index
    Given I have a repository
      And I stage 6 features      
      And I modify a feature
     When I run the command "diff"
     Then the response should contain "Points/Points.1"       
      And the response should contain "POINT (1 2)"
      And the response should contain "POINT (1 1)"
      And the response should contain "1000"
      And the response should contain "1001"
      And the response should contain "StringProp1_1"  
      And the response should contain "StringProp1_1a"
     
Scenario: Show diff between working tree and index, showing only summary 
    Given I have a repository
      And I stage 6 features      
      And I modify a feature
     When I run the command "diff --raw"
     Then the response should contain "Points/Points.1"   
     
Scenario: Show diff between working tree and index, when no changes have been made 
    Given I have a repository
      And I stage 6 features         
     When I run the command "diff"
     Then the response should contain "No differences found"   
     
Scenario: Show diff between working tree and index, for a single modified tree
    Given I have a repository
      And I stage 6 features   
      And I modify a feature         
     When I run the command "diff -- Points"
     Then the response should contain "Points/Points.1"   
      And the response should contain "POINT (1 1)"
      And the response should contain "1000"
      
Scenario: Show diff between working tree and index, for a single unmodified tree
    Given I have a repository
      And I stage 6 features   
      And I modify a feature         
     When I run the command "diff -- Lines"
	 Then the response should contain "No differences found"   
      
Scenario: Show diff using too many commit refspecs
    Given I have a repository
      And I stage 6 features   
      And I modify a feature         
     When I run the command "diff commit1 commit2 commit3"
	 Then the response should contain "Commit list is too long"   
	 
Scenario: Show diff using a wrong commit refspec
    Given I have a repository
      And I stage 6 features   
      And I modify a feature         
     When I run the command "diff wrongcommit"
	 Then the response should contain "Refspec wrongcommit did not resolve to a tree"  	 
     
Scenario: Show diff between working tree and index, for a single modified tree, showing only summary
    Given I have a repository
      And I stage 6 features   
      And I modify a feature         
     When I run the command "diff -- Points --raw"
     Then the response should contain "Points/Points.1"  
      And the response should not contain "POINT (1 1)"
      And the response should not contain "1000"
     