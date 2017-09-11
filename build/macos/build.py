
templates = ("app", "repository")

ver = input("Enter the product version (eg: 1.0.0): ");	

for name in templates:
	f = open(name + "-Info.plist-template")
	s = f.read()
	f.close()
	s = s.replace("${ver}", ver)
	
	f = open("v" + ver + "-" + name + "-Info.plist", "w")
	f.write(s)
	f.close()
