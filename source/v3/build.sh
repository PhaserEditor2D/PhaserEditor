#!/bin/bash

export VER=3.0.0

echo Cleaning 'dist' folder

rm -Rf dist/$VER
mkdir -p dist/$VER/linux/PhaserEditor2D
mkdir -p dist/$VER/windows/PhaserEditor2D
mkdir -p dist/$VER/macos/PhaserEditor2D

pushd .

echo Building server

cd source/server/src/github.com/PhaserEditor/PhaserEditor2D-Server

echo +Building Linux

export GOOS=linux;export GOARCH=amd64; go build launcher/main.go
mv main ~/Documents/PhaserEditor/Public/source/v3/dist/$VER/linux/PhaserEditor2D/PhaserEditor2D

popd 

pushd .

echo Copying to 'dist' folder

cp -R source/client dist/$VER/linux/PhaserEditor2D/editor

echo Copying to 'bin' folder

rm -Rf bin
mkdir bin
cp -R dist/$VER/linux/PhaserEditor2D bin/