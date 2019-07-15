.. include:: _header.rst

.. sectnum::
   :depth: 3
   :start: 4

Asset Pack Editor
=================

In a Phaser_ game you load the files using any of the `Phaser.Loader`_ methods. Many of these methods require additional information to describe the format of the file:

```
this.load.spritesheet({
    key: "player.png",
    width: 16,
    height: 16
});

```

There is a especial method, the `load.pack()`_ that allows to load the description of the files from a JSON file: the **Asset Pack File**. 

|PhaserEditor|_ provides an editor for the Asset Pack files, making it very easy to load the assets of your game. Instead of spent a precious amount of time writing the ..




Create a new Asset Pack file
----------------------------

Add file keys
-------------

Refactoring: rename and remove file keys
----------------------------------------


Asset Pack state of the project
-------------------------------