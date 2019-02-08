#!/bin/bash

# examples
rm -R /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/*
cp -R /home/arian/Documents/Phaser/phaser3-examples/public/assets /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/
cp -R /home/arian/Documents/Phaser/phaser3-examples/public/src /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/
rm -R /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples.assets.audio/phaser3-examples/public/assets/audio
mv /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/assets/audio /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples.assets.audio/phaser3-examples/public/assets/

echo "Remember to build the examples cache (phasereditor.scripts.BuildExamplesCache)"