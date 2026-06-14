package com.example.meetings.unit;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.example.meetings")
@IncludeTags("unit-tests")
public class UnitTestSuite {
}
