f = open("Info.plist-template")
s = f.read()
f.close()
ver = input("Enter the product version (eg: 1.4.2): ");
s = s.replace("${ver}", ver)
print(s)

f = open("v" + ver + "-Info.plist", "w")
f.write(s)
f.close()