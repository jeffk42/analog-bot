#!/bin/bash

echo "Attempting to recycle the AnalogBot process..."
source ${BASH_SOURCE%/*}/killbot.sh
sleep 2
source ${BASH_SOURCE%/*}/startbot.sh
echo "AnalogBot recycle complete."
exit
