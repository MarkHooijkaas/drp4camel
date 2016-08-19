#!/bin/bash

source run-cp.tmp
MAINCLASS=org.kisst.drp4camel.Main

java -cp ./build/classes/main:$CP $MAINCLASS
