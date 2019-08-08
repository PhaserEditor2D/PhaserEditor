#!/bin/bash

# docs
rm -R /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.metadata/phaser-custom/phaser3-docs/json
pushd /home/arian/Documents/Phaser/phaser3-docs/
npm run json
popd
cp -R /home/arian/Documents/Phaser/phaser3-docs/json /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.metadata/phaser-custom/phaser3-docs/
#npm run tsgen

cd /home/arian/Documents/PhaserEditor/Public/scripts

./minify-phaser-json.js

