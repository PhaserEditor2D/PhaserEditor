#!/usr/bin/node

const fs = require("fs");

const dir = ".";

function walk(parent, parentFileData) {
	const files = fs.readdirSync(parent);

	for (let file of files) {		
		const fullname = parent + "/" + file;
		const stats = fs.statSync(fullname);
		const isDir = stats.isDirectory();
				
		//console.log(fullname);
		
		const fileData = {
			name: file,
			isFile: !isDir,			
		};

		parentFileData.children.push(fileData);

		if (isDir) {
			fileData.children = [];	
			walk(fullname, fileData);
		}		
	}
}

const tree = {
	name: "",
	isFile: false,
	children: []
};

walk(dir, tree);

console.log(JSON.stringify(tree, null, 4));

