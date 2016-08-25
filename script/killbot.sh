#!/bin/bash

# Kill the running analog-bot process.
echo "Killing the AnalogBot process found with process ID `pgrep -f "java -jar .*analog-bot.*"`..."
pkill -f "java -jar .*analog-bot.*"
echo "Complete."

