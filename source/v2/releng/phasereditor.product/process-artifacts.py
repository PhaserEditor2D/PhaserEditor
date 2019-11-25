#!/usr/bin/python3

VER = "2.1.5.20191125"

import os

bin_file = "target/repository/binary/phasereditor2d.com.executable.cocoa.macosx.x86_64_" + VER
stat = os.stat(bin_file)

download_size = str(stat.st_size)

from subprocess import run, PIPE

res = run(["md5sum", bin_file], stdout=PIPE,encoding='utf8')

download_md5 = res.stdout.split(" ")[0]

res = run(["sha256sum", bin_file], stdout=PIPE,encoding='utf8')

download_sha256 = res.stdout.split(" ")[0]

import xml.etree.ElementTree as ET

tree = ET.parse("target/repository/artifacts.xml")
root = tree.getroot()

artifacts = root.iter("artifact")



for artifact in artifacts:
	attrib = artifact.attrib
	id = attrib["id"]
	if id == "phasereditor2d.com.executable.cocoa.macosx.x86_64":		
		for prop in artifact.iter("property"):

			if prop.attrib["name"] == "download.size":
				prop.set("value", download_size)

			elif prop.attrib["name"] == "artifact.size":
				prop.set("value", download_size)

			elif prop.attrib["name"] == "download.md5":
				prop.set("value", download_md5)

			elif prop.attrib["name"] == "download.checksum.md5":
				prop.set("value", download_md5)

			elif prop.attrib["name"] == "download.checksum.sha-256":
				prop.set("value", download_sha256)

tree.write("target/repository/artifacts.xml")