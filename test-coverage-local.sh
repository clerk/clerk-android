#!/bin/bash

echo "=== Local JaCoCo Coverage Test ==="

# Check if Android SDK is configured
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME not set. Attempting to create a minimal local.properties..."
    # Try to find Android SDK in common locations
    for sdk_path in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk" "/opt/android-sdk"; do
        if [ -d "$sdk_path" ]; then
            echo "Found Android SDK at: $sdk_path"
            echo "sdk.dir=$sdk_path" > local.properties
            export ANDROID_HOME="$sdk_path"
            break
        fi
    done
    
    if [ -z "$ANDROID_HOME" ]; then
        echo "❌ Could not find Android SDK. Please set ANDROID_HOME or install Android SDK."
        echo "You can download it from: https://developer.android.com/studio"
        exit 1
    fi
fi

echo "✅ Android SDK configured at: $ANDROID_HOME"

# Clean previous build artifacts
echo "🧹 Cleaning previous build artifacts..."
./gradlew clean

# Run tests
echo "🧪 Running unit tests..."
if ./gradlew :source:testDebugUnitTest --info; then
    echo "✅ Tests completed successfully"
else
    echo "⚠️  Some tests may have failed, but continuing with coverage generation..."
fi

# Generate coverage reports
echo "📊 Generating coverage reports..."
./gradlew :source:jacocoTestReport --info
./gradlew jacocoRootReport --info

# Check results
echo ""
echo "=== Coverage Report Status ==="

if [ -f "source/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml" ]; then
    echo "✅ Individual module coverage report generated"
    INDIVIDUAL_REPORT="source/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
    
    # Extract coverage percentage
    INSTRUCTION_COVERED=$(grep -o 'type="INSTRUCTION".*covered="[0-9]*"' "$INDIVIDUAL_REPORT" | sed 's/.*covered="\([0-9]*\)".*/\1/')
    INSTRUCTION_MISSED=$(grep -o 'type="INSTRUCTION".*missed="[0-9]*"' "$INDIVIDUAL_REPORT" | sed 's/.*missed="\([0-9]*\)".*/\1/')
    
    if [ -n "$INSTRUCTION_COVERED" ] && [ -n "$INSTRUCTION_MISSED" ]; then
        INSTRUCTION_TOTAL=$((INSTRUCTION_COVERED + INSTRUCTION_MISSED))
        if [ $INSTRUCTION_TOTAL -gt 0 ]; then
            INSTRUCTION_PERCENTAGE=$((INSTRUCTION_COVERED * 100 / INSTRUCTION_TOTAL))
            echo "📈 Individual module coverage: $INSTRUCTION_PERCENTAGE%"
        fi
    fi
    
    echo "📁 Individual report: source/build/reports/jacoco/jacocoTestReport/html/index.html"
else
    echo "❌ Individual module coverage report NOT generated"
fi

if [ -f "build/reports/jacoco/jacocoRootReport/jacocoRootReport.xml" ]; then
    echo "✅ Root coverage report generated"
    ROOT_REPORT="build/reports/jacoco/jacocoRootReport/jacocoRootReport.xml"
    
    # Extract coverage percentage
    INSTRUCTION_COVERED=$(grep -o 'type="INSTRUCTION".*covered="[0-9]*"' "$ROOT_REPORT" | sed 's/.*covered="\([0-9]*\)".*/\1/')
    INSTRUCTION_MISSED=$(grep -o 'type="INSTRUCTION".*missed="[0-9]*"' "$ROOT_REPORT" | sed 's/.*missed="\([0-9]*\)".*/\1/')
    
    if [ -n "$INSTRUCTION_COVERED" ] && [ -n "$INSTRUCTION_MISSED" ]; then
        INSTRUCTION_TOTAL=$((INSTRUCTION_COVERED + INSTRUCTION_MISSED))
        if [ $INSTRUCTION_TOTAL -gt 0 ]; then
            INSTRUCTION_PERCENTAGE=$((INSTRUCTION_COVERED * 100 / INSTRUCTION_TOTAL))
            echo "📈 Root project coverage: $INSTRUCTION_PERCENTAGE%"
        fi
    fi
    
    echo "📁 Root report: build/reports/jacoco/jacocoRootReport/html/index.html"
else
    echo "❌ Root coverage report NOT generated"
fi

# Debug information
echo ""
echo "=== Debug Information ==="
echo "🔍 Execution data files found:"
find . -name "*.exec" -type f -ls 2>/dev/null || echo "No .exec files found"

echo "🔍 Test result files:"
find . -path "*/test-results/*" -name "*.xml" -type f | head -5 || echo "No test result files found"

echo "🔍 Build directories:"
ls -la source/build/tmp/kotlin-classes/debug/ 2>/dev/null | head -5 || echo "No debug classes found"

echo ""
echo "=== Summary ==="
if [ -f "build/reports/jacoco/jacocoRootReport/jacocoRootReport.xml" ] || [ -f "source/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml" ]; then
    echo "✅ JaCoCo coverage reports generated successfully!"
    echo "Open the HTML reports in your browser to view detailed coverage information."
else
    echo "❌ No coverage reports were generated. Check the error messages above."
    echo "Common issues:"
    echo "  - Android SDK not properly configured"
    echo "  - Tests not running due to dependencies"
    echo "  - JaCoCo plugin configuration issues"
fi