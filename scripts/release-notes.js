#!/usr/bin/node

const fs = require("fs");
const { exec } = require("child_process");

const {current_ver_tag, next_ver } = require("./versions")

proc = exec("git log --oneline " + current_ver_tag + "..HEAD", {maxBuffer : Number.MAX_VALUE},  (err, stdout, stderr) => {

	console.log(stdout);

});