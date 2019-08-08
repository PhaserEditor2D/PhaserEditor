#!/bin/bash

./update-code.sh
./update-examples.sh
./update-docs.sh

echo
echo
echo "REMEMBER TO:"
echo
echo "1) Build the examples cache (phasereditor.scripts.BuildExamplesCache)"
echo
echo "2) Delete unused audio assets (phasereditor.scripts.RemoveUnusedAudioAssets)"
echo
echo "3) Run the script delete-unussed-assets.js"
echo
echo "4) Build Phaser Chains resources (phasereditor.scripts.BuildChainsOnline)"