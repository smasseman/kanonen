sudo java -Dlogback.configurationFile=src/main/resources/logback.xml -cp ./build/classes/kotlin/main:build/libs/kanonen-0.0.1.jar:$(find build/install | tr '\n' ':') se.smasseman.kanonen.TestOutputKt $1
