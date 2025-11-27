# Assignment 2: Log4j Custom Appender and Layout

**Student Name:** Jiawei Chen  
**Student ID:** 24009020  
**GitHub Repository:** https://github.com/eth43900-code/assign251_2-logger  
**Course:** 159.251 - Software Design and Construction  

## 1. Project Overview

This project implements two core components for Apache Log4j 1.2:

- **MemAppender**: A singleton in-memory appender that stores log events, supports configurable max size, and discards oldest logs when capacity is exceeded.
- **VelocityLayout**: A flexible layout using Apache Velocity template engine for dynamic log formatting, compatible with both custom and built-in Log4j appenders.

The project adheres to Maven standards, implements advanced OOP patterns (Singleton, Dependency Injection), and includes comprehensive unit, integration, and stress tests. It also features JMX monitoring (bonus requirement) for real-time log metrics.

## 2. Prerequisites

- **Java Development Kit (JDK)**: OpenJDK 22.0.1 (compatible with JDK 8+)
- **Apache Maven**: 3.6+ (for build, testing, and dependency management)
- **Profiling Tool**: VisualVM 2.1.6 (for memory/CPU monitoring during stress tests)
- **Libraries**: All dependencies are managed via Maven (Log4j 1.2.17, Velocity 1.7, JUnit 5.10.3, JaCoCo 0.8.14)

## 3. How to Build & Run the Project

### 3.1 Basic Build & Unit Tests

Compile the project, run all unit/integration tests, and generate code coverage reports:

```bash
mvn clean test jacoco:report
```

#### Outputs:

* **Unit Test Results**: Console output (pass/fail status for `MemAppenderTest`, `VelocityLayoutTest`, `IntegrationTest`, etc.)
* **Coverage Report**:
  * HTML: `target/site/jacoco/index.html` (open in any browser)
  * PDF: `coverage.pdf` (root directory, exported from HTML)

### 3.2 Run Stress Test (Performance Profiling)

Execute the dedicated stress test (200,000 logs, 30 concurrent threads) to measure time/memory performance:

```bash
mvn test -Dtest=StressTest
```

#### Key Notes:

* The test runs for ~2-3 minutes and sleeps for 60 seconds to allow VisualVM connection.
* **Profiling Steps (Debug/Monitoring)**:
  1. Launch VisualVM (included in JDK or downloadable from [https://visualvm.github.io/](https://visualvm.github.io/)).
  2. During the 60-second sleep, select `org.apache.maven.surefire.booter.ForkedBooter` (pid matches the test process) in VisualVM.
  3. Navigate to the **Monitor** tab to capture heap memory/CPU usage screenshots.
  4. For deep analysis, use the **Sampler** tab to profile method execution time.

### 3.3 Troubleshooting & Debug Steps

#### Common Issues:

1. **Test Timeout**:
   
   * Root Cause: Maven Surefire plugin’s default timeout is insufficient for stress tests.
   * Fix: The `pom.xml` already configures a 6-minute timeout (`<timeout>360000</timeout>`). No additional changes needed.

2. **VisualVM Cannot Connect**:
   
   * Ensure the test is in the 60-second sleep phase (check console for "Sleeping for 60 seconds...").
   * Verify the JDK version of VisualVM matches the project’s JDK (OpenJDK 22.0.1).

3. **Coverage Report Missing**:
   
   * Re-run `mvn clean test jacoco:report` to regenerate the report (ensure no test failures).

4. Implementation Details & Marks Mapping

-----------------------------------------

### Task 1: MemAppender (7 Marks)

* **Singleton & Dependency Injection (3.5 marks)**:
  * Singleton: Enforced via `getInstance()` (synchronized) and `resetInstance()` (cleans old instances).
  * DI: Overloaded `getInstance(List<LoggingEvent>)` for log storage list injection; `setLayout()` for layout injection.
* **Info Methods & Preconditions (2 marks)**:
  * `getCurrentLogs()`: Returns unmodifiable list of `LoggingEvent` instances.
  * `getEventStrings()`: Generates formatted strings (requires layout; throws `IllegalStateException` if layout is null).
  * `printLogs()`: Prints formatted logs to console and clears the cache (validates layout presence).
* **maxSize & Features (1.5 marks)**:
  * Configurable via `setMaxSize(int)` (validates positive integer input).
  * Discard logic: Removes oldest logs when size ≥ maxSize; tracks discarded count via `getDiscardedLogCount()` (long type, excludes cleared logs).

### Task 2: VelocityLayout (3 Marks)

* **Velocity Engine (1 mark)**: Initializes `VelocityEngine` with `NullLogChute` to avoid log conflicts.
* **Appender Compatibility (1 mark)**: Tested with `MemAppender`, `ConsoleAppender`, and `FileAppender` (see `IntegrationTest`).
* **Variable Support (1 mark)**: Supports `$c` (logger name), `$d` (date `toString()`), `$m` (message), `$p` (level), `$t` (thread), `$n` (line separator); pattern set via constructor or `setPattern()`.

### Task 3: Testing (4 Marks)

* **JUnit Coverage & Asserts (2 marks)**: JUnit 5 tests with precise assertions (`assertSame`, `assertEquals`, `assertThrows`); JaCoCo reports 94% instruction coverage (MemAppender: 95%, VelocityLayout: 92%).
* **Combinations (1.5 marks)**: `IntegrationTest` validates:
  * MemAppender + VelocityLayout/PatternLayout
  * VelocityLayout + ConsoleAppender
* **Maven Locations (0.5 marks)**: All tests stored in `src/test/java/assign251_2` (compliant with Maven structure).

### Task 4: Stress-Testing (6 Marks)

* **Separate Class (0.5 marks)**: `StressTest.java` (tagged `@Tag("stress")`).
* **Appender Comparisons (2 marks)**: Compares MemAppender (ArrayList/LinkedList) vs. ConsoleAppender/FileAppender; measures time/memory for maxSizes: 100, 500, 1000, 10000, 100000, 1000000.
* **Layout Comparison (1 mark)**: Compares VelocityLayout vs. PatternLayout (200k logs).
* **Report & Analysis (2 marks)**: `performance-analysis.pdf` with tables, VisualVM screenshots, and deep performance insights.
* **Coverage Reports (0.5 marks)**: JaCoCo-generated branch/statement coverage reports.

### Task 5: Build Management (2 Marks)

* Maven manages all dependencies (Log4j, Velocity, JUnit 5) and integrates JaCoCo for coverage.
* Build phases: Compile → Test → Coverage Report → Package.

### Bonus: JMX MBean (2 Marks)

* `MemAppenderMBean` interface exposes:
  
  * `getLogMessages()`: Logs as a string array.
  * `getEstimatedCacheSize()`: Cached logs size (bytes).
  * `getDiscardedLogCount()`: Number of discarded logs.

* Registers with `PlatformMBeanServer` for JMX monitoring.
5. Project Structure

--------------------

plaintext
    assign251_2/
    ├── src/
    │   ├── main/
    │   │   └── java/
    │   │       └── assign251_2/
    │   │           ├── MemAppender.java        # Custom in-memory appender
    │   │           ├── MemAppenderMBean.java   # JMX MBean interface
    │   │           └── VelocityLayout.java     # Velocity-based layout
    │   └── test/
    │       └── java/
    │           └── assign251_2/
    │               ├── MemAppenderTest.java    # Unit tests for MemAppender
    │               ├── VelocityLayoutTest.java # Unit tests for VelocityLayout
    │               ├── IntegrationTest.java    # Integration tests
    │               ├── MemAppenderJmxTest.java # JMX tests
    │               └── StressTest.java         # Stress/performance tests
    ├── pom.xml                                 # Maven build configuration
    ├── README.md                               # Project documentation
    ├── performance-analysis.pdf                # Performance report
    ├──  target/  
    └── coverage/coverage.pdf                            # Code coverage report

6. Generated Reports

--------------------

* **performance-analysis.pdf**: Detailed performance metrics, VisualVM screenshots, and analysis of stress test results (root directory).
* **coverage.pdf**: JaCoCo code coverage report (root directory); HTML version at `target/site/jacoco/index.html`.
