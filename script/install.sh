#!/bin/bash
echo "Starting the AnalogBot installer..."

jarfile="analog-bot-0.6.0-jar-with-dependencies.jar"
currentdir=${BASH_SOURCE%/*}

echo Current directory is $currentdir

if [ -z "$1" ]; then 
    echo Usage: $0 /path/to/installdir
    exit
fi

installdir=$1

if [ ! -d $installdir ]; then
    echo The selected install location $installdir is not a directory. Please specify an existing location to place the analog-bot install directory.
    exit
fi

if [ ! -w $installdir ]; then
    echo The installer needs write permission to $installdir in order to complete the installation.
    exit
fi

cd $currentdir/..

if [ ! -e "pom.xml" ]; then
    echo A pom.xml file was not found in the expected location `pwd`, exiting.
    exit
fi


mvn package

echo Create analog-bot directory in $installdir...
installdir=$installdir/analog-bot
mkdir -vp $installdir

echo Copy files...
cp -v script/startbot.sh $installdir
cp -v script/killbot.sh $installdir
cp -v script/recyclebot.sh $installdir
cp -v script/buildstats.sh $installdir

#propdir=$installdir/properties
#mkdir -v $propdir
cp -v properties/analogbot.properties.default $installdir/analogbot.properties
cp -v properties/logging.properties $installdir

scheddir=$installdir/schedule
mkdir -v $scheddir
cp -v script/cron_schedule.txt $scheddir
cp -v target/$jarfile $installdir

echo Installation complete.
exit
