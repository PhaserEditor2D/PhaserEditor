#!/usr/bin/node

const fs = require("fs");
const { exec } = require("child_process");

const {current_ver_tag, next_ver } = require("./versions")

console.log("Listing plugins with version " + next_ver + "\n");

proc = exec("git diff --name-only HEAD " + current_ver_tag, {maxBuffer : Number.MAX_VALUE},  (err, stdout, stderr) => {
	var lines = stdout.split("\n");

	var plugin_names = new Set();

	for(var line of lines) {
		if (line.startsWith("source/v2/phasereditor/")) {
			var parts = line.split("/");
			var plugin = parts[3];	

			plugin_names.add(plugin);
		}
	}

	
	plugin_names.delete("Scripts");
	plugin_names.add("phasereditor.lic");


	for(var plugin_name of plugin_names) {
		//console.log("Processing: " + plugin_name);

		let plugin_path = "../source/v2/phasereditor/" + plugin_name;

		if (plugin_name === "phasereditor.lic") {
			plugin_path = "../../PhaserEditor_private/source/" + plugin_name;
		}

		if (!fs.existsSync(plugin_path)) {
			//console.log("Deleted!\n" + plugin_path);
			continue;
		}
		
		if (!plugin_name.endsWith(".features")) {
			let text = fs.readFileSync(plugin_path + "/META-INF/MANIFEST.MF", {encoding: "utf-8"});
			for(let line of text.split("\n")) {
				if (line.startsWith("Bundle-Version:")) {
					let ver = line.substring(16).trim();
					if (ver === next_ver) {
						console.log("Updated plugin: " + plugin_name + "_" + ver);						
					}
				}
			}			
		}
	}	

	console.log("\nDone!\n");	
});




