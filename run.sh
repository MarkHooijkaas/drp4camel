#!/bin/bash

source run-cp.tmp
MAINCLASS=Main

java -cp ./build/classes/main:./build/classes/test:$CP $MAINCLASS
