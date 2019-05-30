#!/bin/bash

export root=~/Documents/PhaserEditor
export ver=2.1.1
export fullver=2.1.1.20190530
export prever=2.1.0
export fullprever=2.1.0.20190527

echo ----------------------------------------
echo Applying Phaser Editor 2D products patch
echo ----------------------------------------
echo $ver - $fullver

echo
echo


echo :: Testing Info.plist has the right version...
echo
if cat target-patch/products/phasereditor2d.com/macosx/cocoa/x86_64/PhaserEditor2D.app/Contents/Info.plist | grep $ver; then
	echo +Info.plist ok!
else 
	echo +Wrong Info.plist version. Good bye!
	exit;	
fi
echo
echo 


echo :: Testing phasereditor2d.com.product has the right version...
echo

if cat phasereditor2d.com.product | head -n 12 | grep $fullprever; then	
	echo +Wrong .product file version. It contains previous $fullprever. Good bye!
	exit;	
fi

if cat phasereditor2d.com.product | head -n 12 | grep $ver; then
	echo .product file version ok!
else
	echo +Wrong .product file version. It does not contains $ver. Good bye!
	exit;	
fi
echo
echo 


echo Copying target-path/ ...
cp -rf target-patch/* target/

echo Rehashing repository/binary

unxz target/repository/artifacts.xml.xz
./process-artifacts.py
pushd .
cd target/repository/
zip -rq artifacts.jar artifacts.xml
popd
xz target/repository/artifacts.xml


echo
echo :: Set execution permissions
echo

echo +++ Linux
pushd .
cd target/products/phasereditor2d.com/linux/gtk/x86_64/PhaserEditor2D/
./SetExecPermissions.sh
popd

echo

echo +++ macOS
pushd .
cd target/products/phasereditor2d.com/macosx/cocoa/x86_64/
./SetExecPermissions.sh
popd

echo

echo
echo :: Zipping products
echo

rm -Rf $root/Releases/v$ver/dist/
mkdir -p $root/Releases/v$ver/dist/

echo +++ Linux
pushd .
cd target/products/phasereditor2d.com/linux/gtk/x86_64/
zip -rq compressed *
md5sum compressed.zip > compressed.zip.md5
mkdir -p $root/Releases/v$ver/dist/PhaserEditor2D-$ver-linux
mv PhaserEditor2D $root/Releases/v$ver/dist/PhaserEditor2D-$ver-linux/
mv compressed.zip $root/Releases/v$ver/dist/PhaserEditor2D-$ver-linux.zip
mv compressed.zip.md5 $root/Releases/v$ver/dist/PhaserEditor2D-$ver-linux.zip.md5
popd

echo
echo +++ Windows
pushd .
cd target/products/phasereditor2d.com/win32/win32/x86_64/
rm PhaserEditor2D/eclipsec.exe
zip -rq compressed *
md5sum compressed.zip > compressed.zip.md5
mkdir -p $root/Releases/v$ver/dist/PhaserEditor2D-$ver-windows
mv PhaserEditor2D $root/Releases/v$ver/dist/PhaserEditor2D-$ver-windows/
mv compressed.zip $root/Releases/v$ver/dist/PhaserEditor2D-$ver-windows.zip
mv compressed.zip.md5 $root/Releases/v$ver/dist/PhaserEditor2D-$ver-windows.zip.md5
popd

echo
echo +++ macOS
pushd .
cd target/products/phasereditor2d.com/macosx/cocoa/
mv x86_64 PhaserEditor2D
zip -rq compressed *
md5sum compressed.zip > compressed.zip.md5
mv compressed.zip $root/Releases/v$ver/dist/PhaserEditor2D-$ver-macos.zip
mv compressed.zip.md5 $root/Releases/v$ver/dist/PhaserEditor2D-$ver-macos.zip.md5
mkdir -p $root/Releases/v$ver/dist/PhaserEditor2D-$ver-macos
mv PhaserEditor2D $root/Releases/v$ver/dist/PhaserEditor2D-$ver-macos/
popd

echo 
echo :: Copying repository/
echo
rm -rf $root/Releases/v$ver/repository
mv target/repository $root/Releases/v$ver/dist/repository