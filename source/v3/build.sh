#!/bin/bash

export VER=3.0.0

echo Cleaning 'dist' folder

rm -Rf dist/$VER
mkdir -p dist/$VER/linux/PhaserEditor2D
mkdir -p dist/$VER/windows/PhaserEditor2D
mkdir -p dist/$VER/macos/PhaserEditor2D

pushd .

echo Building server

cd source/server/src

echo +Building Linux

export GOOS=linux;export GOARCH=amd64; go build PhaserEditor2D.go
mv PhaserEditor2D ../../../dist/$VER/linux/PhaserEditor2D/PhaserEditor2D-Server

popd 

pushd .

echo Copying to 'dist' folder

cp -R source/client dist/$VER/linux/PhaserEditor2D/editor

echo Copying to 'bin' folder

rm -Rf bin
mkdir bin
cp -R dist/$VER/linux/PhaserEditor2D bin/