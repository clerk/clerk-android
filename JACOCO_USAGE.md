# JaCoCo Code Coverage Integration

This project has been configured with JaCoCo code coverage analysis.

## Available Tasks

### Individual Module Coverage
Run tests and generate coverage report for a specific module:
```bash
./gradlew :source:jacocoTestReport
```

### Root/Combined Coverage
Generate a combined coverage report across all modules:
```bash
./gradlew jacocoRootReport
```

### Running with Tests
The coverage reports are automatically generated when you run:
```bash
./gradlew check
```

## Report Locations

- **Individual module reports**: `[module]/build/reports/jacoco/jacocoTestReport/html/index.html`
- **Combined report**: `build/reports/jacoco/jacocoRootReport/html/index.html`

## Configuration

### Coverage is enabled for:
- Unit tests (`testDebugUnitTest`)
- Android instrumentation tests (when run)

### Excluded from coverage:
- Generated files (`R.class`, `BuildConfig.*`)
- Android framework files
- Test files
- Kotlin serialization generated classes

## Thresholds

To add minimum coverage thresholds, you can configure the `jacocoTestReport` task in each module's `build.gradle.kts`:

```kotlin
tasks.named<JacocoReport>("jacocoTestReport") {
  // ... existing configuration ...
  
  doLast {
    val report = file("${reportsDir}/jacoco/jacocoTestReport/jacocoTestReport.xml")
    // Add your coverage verification logic here
  }
}
```

## Integration with CI/CD

The XML reports can be consumed by various CI/CD platforms and tools:
- SonarQube
- Codecov
- Coveralls
- GitHub Actions with coverage badges

Example for GitHub Actions:
```yaml
- name: Generate Code Coverage Report
  run: ./gradlew jacocoTestReport

- name: Upload coverage reports to Codecov
  uses: codecov/codecov-action@v3
  with:
    files: ./source/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
```