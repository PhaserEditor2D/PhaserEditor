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
		
		if (elem.kind === 'package' 
			|| elem.kind === 'module' 
			|| elem.kind === 'event' 
			|| elem.kind === 'typedef'
			) {
			continue;
		}
		
		if (elem.kind === 'class' && elem.scope !== 'static') {
			continue;
		}
		
		if (elem.meta) {
			delete elem.meta.code;
			delete elem.meta.vars;
		}
		
		delete elem.comment;
		delete elem.___id;
		
		result.push(elem);
	}
	
    var fs = require('fs');
    fs.writeFileSync(path.join(env.opts.destination, 'docs.json'), JSON.stringify(result), 'utf8');
};
