#!/usr/bin/node

var fs = require("fs");

var jsonPath = "/home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.metadata/phaser-custom/phaser3-docs/json/phaser.json";


fs.readFile(jsonPath, 'utf8', processFile);

function processFile(err, data) {
	var json = JSON.parse(data);

	
	for(var entry of json.docs) {		
		if (entry.description) {
			if (entry.description.indexOf("prettyprint") > 0) {
				console.log(entry.longname);
			}
		}
	}
}
