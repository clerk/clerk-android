# JaCoCo Coverage Integration for GitHub Actions

This repository now includes comprehensive JaCoCo coverage integration that runs automatically on every pull request.

## 📋 Available Workflows

### 1. Basic Coverage Workflow (`coverage-active.yml`) - **ACTIVE**
- **Trigger**: Runs on every PR to `main` branch
- **Features**:
  - Executes unit tests and generates JaCoCo coverage reports
  - Calculates coverage percentages (instructions, branches, lines)
  - Posts coverage results as PR comments
  - Uploads coverage reports as artifacts
  - Enforces minimum coverage threshold (70%)

### 2. Advanced Coverage Workflow (`coverage-advanced-optional.yml`) - **OPTIONAL**
- **Trigger**: Runs on every PR to `main` branch (when activated)
- **Features**:
  - All features from basic workflow
  - Dynamic coverage badges in PR comments
  - Differential coverage reporting (shows coverage for changed files)
  - Integration with Codecov for trend analysis
  - Enhanced PR comments with coverage guidelines
  - Quality gate with detailed pass/fail messages

## 🚀 Getting Started

### ✅ Ready to Use!
The basic coverage workflow is **already active** and will run automatically on every PR to the `main` branch. No additional setup required!

### 🔧 Want Advanced Features?
To use the advanced workflow instead:

1. **Set up Codecov integration** (optional but recommended):
   - Sign up at [codecov.io](https://codecov.io)
   - Add `CODECOV_TOKEN` to your repository secrets

2. **Switch to advanced workflow**:
   ```bash
   # Deactivate basic workflow
   mv .github/workflows/coverage-active.yml .github/workflows/coverage-basic.yml
   
   # Activate advanced workflow
   mv .github/workflows/coverage-advanced-optional.yml .github/workflows/coverage-active.yml
   ```

## 🔧 Configuration

### Coverage Thresholds
You can adjust the minimum coverage threshold in the workflow files:

```yaml
# In the "Coverage Check" or "Coverage Quality Gate" step
MINIMUM_COVERAGE=70  # Change this value
```

### Different Coverage Types
The workflows track three types of coverage:
- **Instructions**: JVM bytecode instructions
- **Branches**: Decision points in code
- **Lines**: Source code lines

### Customizing Coverage Rules
To modify what gets included/excluded from coverage, edit the JaCoCo configuration in your `build.gradle.kts` files.

## 📊 What You'll See in PRs

### Coverage Comments
Every PR will automatically get a comment showing:
- Current coverage percentages
- Pass/fail status against minimum thresholds
- Links to detailed reports
- Coverage guidelines

### Workflow Artifacts
After each run, you can download:
- HTML coverage reports (for detailed file-by-file analysis)
- XML coverage reports (for integration with other tools)

## 📈 Coverage Reports

### Viewing Reports
1. **In PR Comments**: Summary view with key metrics
2. **In Workflow Artifacts**: 
   - Download the `coverage-reports` artifact
   - Open `build/reports/jacoco/jacocoRootReport/html/index.html`
3. **In Codecov** (if configured): Trend analysis and history

### Report Structure
- **Combined Report**: `build/reports/jacoco/jacocoRootReport/`
- **Module Reports**: `source/build/reports/jacoco/jacocoTestReport/`

## 🛠️ Troubleshooting

### Common Issues

1. **Coverage file not found**
   - Ensure tests are running successfully
   - Check that `jacocoRootReport` task is configured correctly

2. **Permission denied errors**
   - Verify the workflow has the correct permissions:
     ```yaml
     permissions:
       contents: read
       pull-requests: write
       checks: write
     ```

3. **Codecov upload failures**
   - Check that `CODECOV_TOKEN` is set in repository secrets
   - Verify the XML report path is correct

### Manual Testing
You can run coverage locally:
```bash
# Run tests and generate coverage
./gradlew testDebugUnitTest jacocoRootReport

# View the report
open build/reports/jacoco/jacocoRootReport/html/index.html
```

## 🎯 Best Practices

1. **Set Reasonable Thresholds**: Start with 70% and gradually increase
2. **Focus on Critical Code**: Ensure business logic has high coverage
3. **Review Coverage Trends**: Use Codecov or similar tools to track improvements
4. **Don't Chase 100%**: Aim for meaningful coverage, not just high numbers
5. **Test Quality Matters**: Coverage is about test quality, not just quantity

## 📋 Workflow Integration

### Existing Workflows
The coverage workflows complement your existing workflows:
- `android.yml`: Builds the project
- `android-test.yml`: Runs unit tests  
- `coverage-active.yml`: Analyzes test coverage (currently active)
- `coverage-advanced-optional.yml`: Enhanced coverage analysis (optional)

### Workflow Dependencies
Coverage workflows can run in parallel with other checks, making your CI/CD pipeline efficient.

## 🔄 Maintenance

### Updating Coverage Thresholds
As your codebase matures, consider:
- Gradually increasing minimum coverage requirements
- Setting different thresholds for different modules
- Implementing stricter rules for new code vs. legacy code

### Monitoring Coverage Trends
- Review coverage reports regularly
- Identify areas needing more tests
- Celebrate coverage improvements in team meetings

---

*This integration uses JaCoCo ${jacoco.version} and follows Android development best practices.*