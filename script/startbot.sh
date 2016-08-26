#!/bin/bash
echo "Starting AnalogBot from ${BASH_SOURCE%/*} ..."
mkdir -p ${BASH_SOURCE%/*}/log
nohup java -jar ${BASH_SOURCE%/*}/analog-bot-0.5.0-jar-with-dependencies.jar > ${BASH_SOURCE%/*}/log/nohup.out &
echo "AnalogBot process created with process ID `pgrep -nf "java -jar .*analog-bot.*"`."
