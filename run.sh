#!/bin/bash
CP=Users/mark/workspace/cameltest/build/classes/main:/Users/mark/workspace/cameltest/build/resources/main:/Users/mark/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/1.7.21/139535a69a4239db087de9bab0bee568bf8e0b70/slf4j-api-1.7.21.jar:/Users/mark/.gradle/caches/modules-2/files-2.1/ch.qos.logback/logback-classic/1.1.7/9865cf6994f9ff13fce0bf93f2054ef6c65bb462/logback-classic-1.1.7.jar:/Users/mark/.gradle/caches/modules-2/files-2.1/org.apache.camel/camel-core/2.15.6/1995d90d741d4560385ea314a6da2efb3f86b7e4/camel-core-2.15.6.jar:/Users/mark/.gradle/caches/modules-2/files-2.1/org.codehaus.groovy/groovy-all/2.3.11/f6b34997d04c1538ce451d3955298f46fdb4dbd4/groovy-all-2.3.11.jar:/Users/mark/.gradle/caches/modules-2/files-2.1/ch.qos.logback/logback-core/1.1.7/7873092d39ef741575ca91378a6a21c388363ac8/logback-core-1.1.7.jar:/Users/mark/.gradle/caches/modules-2/files-2.1/com.sun.xml.bind/jaxb-core/2.2.11/c3f87d654f8d5943cd08592f3f758856544d279a/jaxb-core-2.2.11.jar:/Users/mark/.gradle/caches/modules-2/files-2.1/com.sun.xml.bind/jaxb-impl/2.2.11/a49ce57aee680f9435f49ba6ef427d38c93247a6/jaxb-impl-2.2.11.jar
MAINCLASS=org.kisst.drp4camel.Main

java -cp ./build/classes/main:$CP $MAINCLASS