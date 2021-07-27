#!/bin/bash

rm SingleJUnitTestRunner.jar
export JAVA_HOME="/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home"
javac SingleJUnitTestRunner.java -cp "junit-4.8.2.jar"
jar cf SingleJUnitTestRunner.jar SingleJUnitTestRunner.class 
rm SingleJUnitTestRunner.class