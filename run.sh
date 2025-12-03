#!/bin/bash

# Finance Tracker Run Script
# This script runs the application directly with Java

cd "$(dirname "$0")"

# Ensure project is compiled
if [ ! -d "target/classes" ]; then
    echo "Compiling project..."
    mvn clean compile -q
fi

# Get classpath
CLASSPATH="target/classes:$(cat classpath.txt)"

# Find JavaFX JARs in Maven repository
JAVAFX_BASE=$(find ~/.m2/repository/org/openjfx/javafx-base/17.0.2 -name "javafx-base-17.0.2-mac-aarch64.jar" 2>/dev/null | head -1)
JAVAFX_CONTROLS=$(find ~/.m2/repository/org/openjfx/javafx-controls/17.0.2 -name "javafx-controls-17.0.2-mac-aarch64.jar" 2>/dev/null | head -1)
JAVAFX_FXML=$(find ~/.m2/repository/org/openjfx/javafx-fxml/17.0.2 -name "javafx-fxml-17.0.2-mac-aarch64.jar" 2>/dev/null | head -1)
JAVAFX_GRAPHICS=$(find ~/.m2/repository/org/openjfx/javafx-graphics/17.0.2 -name "javafx-graphics-17.0.2-mac-aarch64.jar" 2>/dev/null | head -1)

# Build module path
MODULE_PATH="$JAVAFX_BASE:$JAVAFX_CONTROLS:$JAVAFX_FXML:$JAVAFX_GRAPHICS"

# JavaFX modules
JAVAFX_MODULES="javafx.base,javafx.controls,javafx.fxml,javafx.graphics"

echo "Starting Finance Tracker..."
echo "Module path: $MODULE_PATH"

# Run the application
java --module-path "$MODULE_PATH" \
     --add-modules "$JAVAFX_MODULES" \
     --add-opens javafx.graphics/com.sun.glass.ui.mac=ALL-UNNAMED \
     --add-opens javafx.base/com.sun.javafx.runtime=ALL-UNNAMED \
     -cp "$CLASSPATH" \
     com.financetracker.MainApp
