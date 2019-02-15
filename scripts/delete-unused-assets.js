#!/usr/bin/node

var fs = require("fs");

var jsonPath = "/home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.metadata/phaser-custom/examples/examples-cache.json";


fs.readFile(jsonPath, 'utf8', processFile);

var assetsRoot = "/home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/assets";

var usedAssetFiles = new Set();


function processFile(err, data) {
	var json = JSON.parse(data);

	for(let cat of json.examplesCategories) {
		processCategory(cat);	
	}

	console.log("Used files " + usedAssetFiles.size);
	
	deleteFiles("/home/arian/Documents/PhaserEditor/Public/source/v2/phasereditor/phasereditor.resources.phaser.examples/phaser3-examples/public/assets");

}

function deleteFiles(path) {
	var names = fs.readdirSync(path);

	for(let name of names) {
		let file = path + "/" + name;
		var stats = fs.statSync(file);
		
		if (stats.isFile()) {
			let relpath = file.substring(assetsRoot.length - 6);
			let used =  usedAssetFiles.has(relpath);
			if (!used) {
				console.log("Deleting: " + relpath);
				fs.unlinkSync(file);
			}			
		} else {
			deleteFiles(file);
		}
		
	}
}

function processCategory(cat) {
	for(let subcat of cat.subCategories) {
		processCategory(subcat);
	}

	for(let example of cat.examples) {
		for(let mapping of example.map) {
			let file = mapping.orig;
			
			if (file.startsWith("assets/")) {
				usedAssetFiles.add(file);					
			}
		}
	}

}


