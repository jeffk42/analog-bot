#!/bin/bash
echo "Starting WeeklyStatisticsGenerator from ${BASH_SOURCE%/*} ..."
mkdir -p ${BASH_SOURCE%/*}/stats
nohup java -cp ${BASH_SOURCE%/*}/analog-bot-0.6.0-jar-with-dependencies.jar jmk.reddit.weeklystats.WeeklyStatisticsGenerator `date +%s` 1 > ${BASH_SOURCE%/*}/log/nohup.out &
