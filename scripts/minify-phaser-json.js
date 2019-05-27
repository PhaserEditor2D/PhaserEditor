#!/usr/bin/node

console.log("Minifying phaser3-docs/json/phaser.json");

var fs = require("fs");

var jsonPath = "/home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.metadata/phaser-custom/phaser3-docs/json/phaser.json";


fs.readFile(jsonPath, 'utf8', processFile);

function processFile(err, data) {
	var json = JSON.parse(data);

	var newDocs = [];

	for(var entry of json.docs) {

		if (entry.access === "private" || entry.access === "protected") {
			continue;
		}

		delete entry.author;
		delete entry.copyright;
		delete entry.license;
		delete entry.___id;
		delete entry.tags;
		delete entry.comment;

		var meta = entry.meta;
		
		if (meta) {
			delete meta.code;
			delete meta.vars;
		}

		newDocs.push(entry);		
	}


	var newJson = { docs: newDocs };

	var newData = JSON.stringify(newJson);


	fs.writeFile(jsonPath, newData, err => {
		console.log("Done!");
	});

}
