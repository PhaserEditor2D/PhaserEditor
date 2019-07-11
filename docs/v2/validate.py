#!/usr/bin/python3

import re
import os

fail = False

print('''
Validating eclipse help links
''')

rst_files = [n for n in os.listdir(".") if n.endswith(".rst")]

eclipse_help_pattern = re.compile("htt.*://help.eclipse.org.*>")
phaser_help_pattern = re.compile("htt.*://photonstorm.github.io/phaser3-docs.*>")

for name in rst_files:
	
	print("\n=== " + name + " ===\n")
	
	f = open(name)
	s = f.read()
	
	print("\tTesting Eclipse Help links")

	res = eclipse_help_pattern.findall(s)

	for url in res:
		prefixes = ["https://help.eclipse.org/2019-06/topic/", "https://help.eclipse.org/2019-06/nav/", "https://help.eclipse.org/2019-06/index.jsp"]
		i = 0
		for prefix in prefixes:
			if url.startswith(prefix):
				i += 1
		if i == 0:
			print("\t\tERROR: " + url + " --- " + name)			
			fail = True



	print("\tTesting Phaser Docs links")

	res = phaser_help_pattern.findall(s)

	for url in res:
		prefixes = ["https://photonstorm.github.io/phaser3-docs/docs"]
		i = 0
		for prefix in prefixes:
			if url.startswith(prefix):
				i += 1
		# if found a bar prefix
		if i > 0:
			print("\t\tERROR: " + url + " --- " + name)			
			fail = True

	
	f.close()



if fail:
	print("\n\nThe test failed!\n\n")



