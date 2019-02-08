#!/usr/bin/node

console.log("Minifying phaser3-docs/json/phaser.json");

var fs = require("fs");

var jsonPath = "/home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.metadata/phaser-custom/phaser3-docs/json/phaser.json";


fs.readFile(jsonPath, 'utf8', processFile);

function processFile(err, data) {
	var json = JSON.parse(data);

	
	for(var entry of json.docs) {		
		if (entry.fires) {
			console.log(entry.longname);
			for(var fire of entry.fires) {
				console.log("\t" + fire);
			}
			console.log("");
		}
	}
}
