#!/bin/bash
if [ -z "$1" ]; then 
    exit
else
    command_name=$1
    now_date=$2
    subcommand=$3
fi

echo "Starting WeeklyStatisticsGenerator from ${BASH_SOURCE%/*} ..."
cd ${BASH_SOURCE%/*}/
mkdir -p ${BASH_SOURCE%/*}/stats
java -cp ./analog-bot-0.6.0-jar-with-dependencies.jar jmk.reddit.weeklystats.WeeklyStatisticsGenerator $command_name $now_date $subcommand
cd -
