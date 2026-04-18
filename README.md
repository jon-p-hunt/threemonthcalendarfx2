README.md
1. Set your JavaFX SDK path
set PATH_TO_FX=C:\Users\account\Documents\javafx-sdk-25.0.2\lib
2. Compile and run manually 
javac --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.graphics ThreeMonthCalendar.java
java  --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.graphics ThreeMonthCalendar
