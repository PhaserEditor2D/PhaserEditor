#!/usr/bin/node

var fs = require("fs");

var jsonPath = "/home/arian/Documents/Phaser/phaser3-docs/json/phaser.json";


fs.readFile(jsonPath, 'utf8', processFile);

function processFile(err, data) {
	var json = JSON.parse(data);

	for(var entry of json.docs) {

		if (entry.since) {
			console.log(entry.since + " " + entry.longname);	
		}		
	}
}
