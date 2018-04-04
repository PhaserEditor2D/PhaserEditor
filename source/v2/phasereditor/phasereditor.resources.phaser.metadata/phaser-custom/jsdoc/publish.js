/**
    @overview Export the jsdoc comments in a json file
    @version 1.0
 */
'use strict';

var path = require('jsdoc/path');

/**
    @param {TAFFY} data
    @param {object} opts
 */
exports.publish = function (data, opts) {
    data({undocumented: true}).remove();
    var root = data().get();
	
	// clean the tree
	var result = [];
	for(var i = 0; i < root.length; i++) {
		var elem = root[i];
		var longname = elem.longname;
		
		if (elem.kind === 'package' 
			|| elem.kind === 'module' 
			|| elem.kind === 'event' 
			|| elem.kind === 'typedef'
			) {
			console.log("Ignore " + longname + " because elem.kind " + elem.kind);
			continue;
		}
		
		if (elem.kind === 'class' && elem.scope !== 'static') {
			console.log("Ignore " + longname + " because elem.kind != static " + elem.kind + " " + elem.scope);
			continue;
		}
		
		if (elem.meta) {
			
			if (elem.meta.path) {
				var k = elem.meta.path.indexOf("phaser-master");
				if (k !== -1) {
					var p = elem.meta.path.substr(k + 14);
					elem.meta.path = p;
				}
			}
						
			delete elem.meta.code;
			delete elem.meta.vars;
		}
		
		delete elem.comment;
		delete elem.___id;
		
		// Arian
		if (longname.indexOf("~") !== -1) {
			console.log("Ignore " + longname + " because ~");
			continue;
		}
		
		if (elem.access === "private") {
			console.log("Ignore " + longname + " because is private");
			continue;
		}
		
		result.push(elem);
	}	
	
	console.log("");
	console.log("Accepted " + result.length + " / " + root.length + " - " + Number.parseInt((result.length / root.length) * 100) + "%") ;
	
    var fs = require('fs');
    fs.writeFileSync(path.join(env.opts.destination, 'docs.json'), JSON.stringify(result), 'utf8');
};
