# Gradle Wrapper Jar

This file is intentionally missing because the sandbox environment blocks binary downloads.

To build this mod locally, you have two options:

1. **Install Gradle 9.5.1 locally** and run:
   ```
   gradle wrapper --gradle-version 9.5.1
   ./gradlew build
   ```

2. **Download the wrapper jar manually**:
   ```
   curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v9.5.1/gradle/wrapper/gradle-wrapper.jar
   ./gradlew build
   ```

The wrapper properties point to `gradle-9.5.1-bin.zip`, which will be auto-downloaded by Gradle wrapper.
