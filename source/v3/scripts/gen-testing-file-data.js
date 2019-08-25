#!/usr/bin/node

const fs = require("fs");

const dir = ".";

function walk(parent, parentFileData) {
	const files = fs.readdirSync(parent);

	for (let file of files) {		
		const fullname = parent + "/" + file;
		const stats = fs.statSync(fullname);
		const isDir = stats.isDirectory();
				
		const fileData = {
			name: file,
			isFile: !isDir,			
		};

		parentFileData.children.push(fileData);

		if (isDir) {
			fileData.children = [];	
			walk(fullname, fileData);
		} else {
			fileData.contentType = getContentType(fullname);
		}	
	}
}

const contentTypeByExtension = {
	"png" : "img",
	"jpg" : "img",
	"bmp" : "img",
	"gif" : "img",
	"svg" : "img",
	"webp" : "img",
	"mp3": "sound",
	"wav": "sound",	
	"ogg": "sound",
	"mp4": "video",
	"ogv": "video",
	"mpg": "video",
	"webm": "video",
	"avi": "video",
	"js": "js",
	"ts": "ts",
	"json": "json",
	"txt": "txt",
	"md": "txt",
	"scene": "phasereditor2d.scene",	
};

function getContentType(fullname) {
	const ext = getFileExt(fullname);
	if (ext in contentTypeByExtension) {
		return contentTypeByExtension[ext];
	}
	return "any";
}

function getFileExt(fullname) {
	const i = fullname.lastIndexOf(".");
	if (i === -1) {
		return "";
	}
	return fullname.substring(i + 1);
}

const tree = {
	name: "",
	isFile: false,
	children: []
};

walk(dir, tree);

console.log(JSON.stringify(tree, null, 4));

