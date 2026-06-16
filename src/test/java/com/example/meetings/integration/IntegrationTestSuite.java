package com.example.meetings.integration;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.example.meetings")
@IncludeTags({"light-integration-tests", "heavy-integration-tests"})
public class IntegrationTestSuite {
}
