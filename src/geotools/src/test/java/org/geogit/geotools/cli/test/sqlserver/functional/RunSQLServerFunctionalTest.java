package org.geogit.geotools.cli.test.sqlserver.functional;

import org.junit.runner.RunWith;

import cucumber.junit.Cucumber;


/**
 * Single cucumber test runner. Its sole purpose is to serve as an entry point for junit. Step
 * definitions and hooks are defined in their own classes so they can be reused across features.
 * 
 */
@RunWith(Cucumber.class)
@Cucumber.Options(format = { "pretty", "html:target/cucumber-report" }, strict = true)
public class RunSQLServerFunctionalTest {

}
