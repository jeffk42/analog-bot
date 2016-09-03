#!/bin/bash

echo "Attempting to recycle the AnalogBot process..."
cd ${BASH_SOURCE%/*}/
source ./killbot.sh
sleep 2
source ./startbot.sh
echo "AnalogBot recycle complete."
cd -
exit
