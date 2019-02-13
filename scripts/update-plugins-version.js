const fs = require("fs");
const { exec } = require("child_process");


var current_ver_tag = "v2.0.0";
var next_ver = "2.0.1.20190213";

console.log(next_ver);
console.log("Comparing with version " + current_ver_tag);

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
	
	console.log("\nPlugins that need to be updated:\n");


	let plugins_to_update = new Set();
	let features_names = new Set();

	for(var plugin_name of plugin_names) {
		//console.log("Processing: " + plugin_name);

		let plugin_path = "../source/v2/phasereditor/" + plugin_name;

		if (!fs.existsSync(plugin_path)) {
			// console.log("Deleted!\n");
			continue;
		}
		
		if (plugin_name.endsWith(".features")) {
			features_names.add(plugin_name);
		} else {
			let text = fs.readFileSync(plugin_path + "/META-INF/MANIFEST.MF", {encoding: "utf-8"});
			for(let line of text.split("\n")) {
				if (line.startsWith("Bundle-Version:")) {
					let ver = line.substring(16).trim();
					if (ver !== next_ver) {
						console.log("Invalid plugin version: " + plugin_name + " " + ver);
						plugins_to_update.add(plugin_name);
					}
				}
			}			
		}
	}

	let features_to_update = new Set();

	for(let feature_name of features_names) {

		let text = fs.readFileSync("../source/v2/phasereditor/" + feature_name + "/feature.xml", {encoding: "utf-8"});

		for(let plugin_name of plugins_to_update) {
			
			if (text.indexOf(plugin_name) > 0) {
				
				if (!features_to_update.has(feature_name)) {
					
					features_to_update.add(feature_name);

					if (text.indexOf(next_ver) === -1) {
						console.log("Invalid feature version: " + feature_name);
					}
				}				
			}
		}
	}	

	console.log("\nRemember check lic plugin.\n");
});




