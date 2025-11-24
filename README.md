# Assignment 2: Log4j Custom Appender and Layout

**Student Name:** [Jiawei Chen]

**Student ID:** [24009020]

**GitHub Repository:** [eth43900-code/assign251_2-logger](https://www.google.com/search?q=https://github.com/eth43900-code/assign251_2-logger "null")

1. **Project Overview**

-------------------

This project implements a custom Log4j 1.2 Appender (`MemAppender`) that stores log events in memory, and a custom Layout (`VelocityLayout`) that uses the Apache Velocity engine to format log messages. The project demonstrates advanced object-oriented patterns (Singleton, Dependency Injection), rigorous unit testing, and performance stress testing.

2. **How to Run the Project**

-------------------------

### Prerequisites

* **Java Development Kit (JDK):** Version 8 or higher (tested on Java [Your Version]).

* **Apache Maven:** For build and dependency management.

### Build and Run Unit Tests

To compile the project, run all unit tests, and generate the code coverage report, execute:
    mvn clean test jacoco:report

**Output:**

* **Coverage Report:** Open `target/site/jacoco/index.html` in your browser.

* **Test Results:** Check the console output for `IntegrationTest`, `MemAppenderTest`, etc.

### Run the Stress Test (Task 4)

To execute the dedicated performance stress test, which measures time and memory usage for 10,000 logs:
    mvn test -Dtest=StressTest

**Note:** The test will conclude with a _"Sleeping for 30 seconds..."_ message. This is intentional to allow you to connect **VisualVM** to the process to capture a memory snapshot (Heap/CPU monitor) for the report.

3. **Implementation Details & Marks Mapping**

-----------------------------------------

### Task 1: MemAppender (7 Marks)

* **Singleton & DI (3.5 marks):** Enforced via private instance and getInstance(); DI for List<LoggingEvent> via overloaded getInstance(List); Layout via setLayout().
* **Info Methods & Preconditions (2 marks):** getCurrentLogs() (unmodifiable LoggingEvents); getEventStrings() (formatted strings, checks layout != null); printLogs() (prints & clears, checks layout).
* **maxSize & Features (1.5 marks):** Configurable via setMaxSize(); append() discards oldest if full, tracks discardedLogCount (long); cleared logs not counted as discarded.

### Task 2: VelocityLayout (3 Marks)

* **Velocity Engine (1 mark):** Initializes VelocityEngine with NullLogChute.
* **Works with Appenders (1 mark):** Tested with MemAppender and ConsoleAppender.
* **Variables (1 mark):** Supports $c (logger), $d (date toString), $m (message), $p (level), $t (thread), $n (newline); set via constructor or setPattern().

### Task 3: Testing (4 Marks)

* **JUnit Coverage & Asserts (2 marks):** JUnit 5 with precise asserts (e.g., assertSame for events); JaCoCo reports 87-91% instruction coverage.
* **Combinations (1.5 marks):** IntegrationTest shows MemAppender + Velocity/Pattern, Console + Velocity.
* **Maven Locations (0.5 marks):** Tests in src/test/java.

### Task 4: Stress-Testing (6 Marks)

* **Separate Class (0.5 marks):** StressTest.java.
* **Appender Comparisons (2 marks):** Compares MemAppender (ArrayList/LinkedList) vs Console (dummy) vs File (buffered); measures time/memory for maxSizes 1-1e6, before/after full.
* **Layout Comparison (1 mark):** Compares Velocity vs Pattern time.
* **Report & Analysis (2 marks):** performance-analysis.pdf with tables, analysis, VisualVM screenshots.
* **Coverage Reports (0.5 marks):** JaCoCo plugin generates reports.

### Task 5: Build Management (2 Marks)

* Maven manages deps (log4j 1.2.17, velocity 1.7, junit 5.10.3); jacoco for coverage; surefire for tests.

### Bonus: JMX MBean (2 Marks)

* MemAppenderMBean interface; registers with PlatformMBeanServer; monitors logMessages (String[]), estimatedCacheSize (bytes), discardedLogCount.
4. Generated Reports

--------------------

* **performance-analysis.pdf:** Analyzes stress results (time/memory/discarded), with VisualVM heap/GC screenshots. See root directory.
* **coverage.pdf/** JaCoCo report (target/site/jacoco/index.html); high coverage for MemAppender/VelocityLayout.
