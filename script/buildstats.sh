#!/bin/bash
if [ -z "$1" ]; then 
    now_date=`date +%s`
    days_back=7
    command_name='buildstats'
else
    command_name=$1
    now_date=$2
    days_back=$3
fi

echo "Starting WeeklyStatisticsGenerator from ${BASH_SOURCE%/*} ..."
mkdir -p ${BASH_SOURCE%/*}/stats
java -cp ${BASH_SOURCE%/*}/analog-bot-0.6.0-jar-with-dependencies.jar jmk.reddit.weeklystats.WeeklyStatisticsGenerator $command_name $now_date $days_back
