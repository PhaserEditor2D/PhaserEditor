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
mv ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/assets/video/*.* ${PE}/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples.assets.audio/phaser3-examples/public/assets/video/

