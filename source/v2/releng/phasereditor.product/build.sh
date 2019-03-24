#!/bin/bash

export dir=~/Documents/PhaserEditor
export ver=2.0.4
export fullver=2.0.4.20190321

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

echo Copying target-path/ ...
cp -rf target-patch/* target/

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

rm -Rf $dir/Releases/v$ver/dist/
mkdir -p $dir/Releases/v$ver/dist/

echo +++ Linux
pushd .
cd target/products/phasereditor2d.com/linux/gtk/x86_64/
zip -rq compressed *
md5sum compressed.zip > compressed.zip.md5

mv PhaserEditor2D $dir/Releases/v$ver/dist/PhaserEditor2D-$ver-linux
mv compressed.zip $dir/Releases/v$ver/dist/PhaserEditor2D-$ver-linux.zip
mv compressed.zip.md5 $dir/Releases/v$ver/dist/PhaserEditor2D-$ver-linux.zip.md5
popd

echo
echo +++ Windows
pushd .
cd target/products/phasereditor2d.com/win32/win32/x86_64/
zip -rq compressed *
md5sum compressed.zip > compressed.zip.md5
mv PhaserEditor2D $dir/Releases/v$ver/dist/PhaserEditor2D-$ver-windows
mv compressed.zip $dir/Releases/v$ver/dist/PhaserEditor2D-$ver-windows.zip
mv compressed.zip.md5 $dir/Releases/v$ver/dist/PhaserEditor2D-$ver-windows.zip.md5
popd

echo
echo +++ macOS
pushd .
cd target/products/phasereditor2d.com/macosx/cocoa/
mv x86_64 PhaserEditor2D
zip -rq compressed *
md5sum compressed.zip > compressed.zip.md5
mv compressed.zip $dir/Releases/v$ver/dist/PhaserEditor2D-$ver-macos.zip
mv compressed.zip.md5 $dir/Releases/v$ver/dist/PhaserEditor2D-$ver-macos.zip.md5
mv PhaserEditor2D $dir/Releases/v$ver/dist/PhaserEditor2D-$ver-macos
popd

echo 
echo :: Copying repository/
echo
rm -rf $dir/Releases/v$ver/repository
mv target/repository $dir/Releases/v$ver/repository