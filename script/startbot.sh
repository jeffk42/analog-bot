#!/bin/bash
echo "Starting AnalogBot from ${BASH_SOURCE%/*} ..."
cd ${BASH_SOURCE%/*}/
mkdir -p ${BASH_SOURCE%/*}/log
nohup java -jar ./analog-bot-0.6.0-jar-with-dependencies.jar > ./log/nohup.out &
echo "AnalogBot process created with process ID `pgrep -nf "java -jar .*analog-bot.*"`."
cd -
