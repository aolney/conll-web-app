#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR

echo "DOWNLOADING ALL FILES"
wget -c https://s3.amazonaws.com/repo.iis.memphis.edu/raw-data/mateplus.jar
wget -c https://s3.amazonaws.com/repo.iis.memphis.edu/raw-data/anna-3.3.jar
wget -c https://s3.amazonaws.com/repo.iis.memphis.edu/release/edu/memphis/iis/mateplus-models/4.31/mateplus-models-4.31.pom
wget -c https://s3.amazonaws.com/repo.iis.memphis.edu/release/edu/memphis/iis/mateplus-models/4.31/mateplus-models-4.31.jar

echo "INSTALLING MODELS VIA POM"
mvn install:install-file -Dfile=./mateplus-models-4.31.jar -DpomFile=./mateplus-models-4.31.pom

echo "INSTALLING DEPENDENCIES VIA DIRECT MVN"
function install_one() {
    FP="$SCRIPT_DIR/$1"
    echo "Install: $FP"
    echo "as:      edu.memphis.iis:$2:1.0-SNAPSHOT"
    echo ""
    mvn install:install-file -Dfile="$FP" -DgroupId=edu.memphis.iis -DartifactId="$2" -Dversion=1.0-SNAPSHOT -Dpackaging=jar
    echo ""
}
install_one anna-3.3.jar anna-dep
install_one mateplus.jar mateplus-dep

echo "CLEANING UP DOWNLOADED FILES"
rm *.pom *.jar

