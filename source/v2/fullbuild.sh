#!/bin/bash

mvn clean verify

pushd .
cd releng/phasereditor.product
./build.sh
popd