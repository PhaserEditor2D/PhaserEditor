#!/bin/bash

export PE=/home/arian/Documents/PhaserEditor
export P=/home/arian/Documents/Phaser

rm -R ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/*
cp -R ${P}/phaser3-examples/public/assets ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/
cp -R ${P}/phaser3-examples/public/src ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/
cp -R ${P}/phaser3-examples/public/plugins ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/
rm -R ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples.assets.audio/phaser3-examples/public/assets/audio
mv ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/assets/audio ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples.assets.audio/phaser3-examples/public/assets/
mv ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/assets/tests/piano/*.mp3 ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples.assets.audio/phaser3-examples/public/assets/tests/piano/
mv ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/assets/video ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples.assets.audio/phaser3-examples/public/assets/

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
echo