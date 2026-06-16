package com.example.meetings.e2e;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.example.meetings")
@IncludeTags("e2e-tests")
public class E2ETestSuite {
}
