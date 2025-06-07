# EcoFramework Test Setup

This document describes the test infrastructure added to the EcoFramework project.

## Test Dependencies Added

The following test dependencies have been added to `pom.xml`:

- **JUnit Jupiter 5.9.2**: Modern testing framework for Java
- **Mockito 5.1.1**: Mocking framework for unit tests

## Test Structure

Tests are located in `src/test/java/jp/minecraftuser/ecoframework/`

### BasicInfrastructureTest.java

This test class validates that the test setup is working correctly and demonstrates testing of core utility functions:

- **testJunitSetup()**: Verifies JUnit is working
- **testStringUtilities()**: Tests basic string operations
- **testColorCodePattern()**: Tests color code replacement logic
- **testFullWidthSpaceReplacement()**: Tests full-width space replacement
- **testStringMerging()**: Tests string array merging logic

### UtilityAlgorithmTest.java

This test class focuses on the core algorithms used in the EcoFramework utility functions:

- **testColorCodeReplacement()**: Tests color code replacement patterns
- **testFullWidthSpaceReplacement()**: Tests full-width space conversion
- **testCombinedReplacement()**: Tests combined color and space replacement
- **testStringMerging()**: Tests string array merging algorithm
- **testEdgeCases()**: Tests edge cases and special characters

### CommandFrameworkTest.java

This test class demonstrates testing approaches for command framework functionality:

- **testCommandNameValidation()**: Tests command name validation logic
- **testCommandArgumentParsing()**: Tests command argument parsing
- **testPermissionStringGeneration()**: Tests permission string generation
- **testCommandHierarchy()**: Tests command hierarchy validation
- **testTabCompletion()**: Tests tab completion logic

### BukkitIntegrationTest.java

This test class demonstrates how to structure tests for Bukkit-dependent functionality:

- **testMessageFormatting()**: Tests message formatting for console vs player output
- **testParameterizedMessageFormatting()**: Tests parameterized message formatting
- **testTagMessageFormatting()**: Tests tag-based message formatting
- **testSenderTypeDetection()**: Tests command sender type detection
- **testPermissionLogic()**: Tests permission validation and hierarchy

## Running Tests

Due to Spigot dependencies in the main code, the current setup requires the following to run tests:

1. **With full build**: `mvn test` (requires Spigot API access)
2. **Standalone**: Tests can be compiled and run independently once JUnit is available

## Test Implementation Strategy

The tests focus on:

1. **Pure Java logic**: Testing utility functions that don't depend on Bukkit/Spigot
2. **Algorithm validation**: Verifying core string processing and data manipulation
3. **Infrastructure validation**: Ensuring test framework is properly configured

## Future Test Additions

Potential areas for expanded testing:

1. **Command Framework**: Test command parsing and validation
2. **Configuration Management**: Test config loading and validation
3. **Database Operations**: Test database connection and query logic (with mocks)
4. **Event Handling**: Test listener functionality (with mocks)

## Notes

- Tests are designed to work with minimal dependencies
- Bukkit-dependent code requires mocking for proper unit testing
- The test structure follows standard Maven conventions