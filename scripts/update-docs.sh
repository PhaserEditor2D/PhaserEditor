#!/bin/bash

# docs
rm -R /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.metadata/phaser-custom/phaser3-docs/json
cd /home/arian/Documents/Phaser/phaser3-docs/
#npm run json
cp -R /home/arian/Documents/Phaser/phaser3-docs/json /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.metadata/phaser-custom/phaser3-docs/
#npm run tsgen
cp -R /home/arian/Documents/Phaser/phaser3-docs/typescript /home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.code/phaser-master/

cd /home/arian/Documents/PhaserEditor/Public/scripts

./minify-phaser-json.js

