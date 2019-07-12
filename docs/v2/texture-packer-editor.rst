.. include:: _header.rst
   
.. sectnum::
   :depth: 3
   :start: 3

Texture Packer Editor
=====================

The packing of images in a textures atlas is a widely used technique for game development. It improves the game performance (especially in a WebGL context) and reduce the size of the game (a plus for browser games).

There are several tools, free and commercial, to create a texture atlas. In the Phaser_ community, the `TexturePacker <https://www.codeandweb.com/texturepacker>`_ developed by `CodeAndWeb <https://www.codeandweb.com>`_ is very popular and has built-in support for Phaser_.

|PhaserEditor|_ has its own texture packer, based on the `LibGDX tools <https://github.com/libgdx/libgdx/tree/master/extensions/gdx-tools>`_. It uses the MaxRects  algorithm, that provides a high packing ratio.

Create a new atlas file
-----------------------

The Texture Packer uses ``.atlas`` files as configurations. To pack your images, you should create an atlas file with the wizard **New Texture Packer File**.

`Learn more about resource wizards <workbench.html#resource-wizards>`_

The **Texture Packer File** wizard shows two pages. The first page is common to all the file wizards, you should set the name and path of the file. The second page allows you to automatically add the generated atlas files (``.json`` + ``.png``) in a selected `pack file <asset-pack-editor.html>`_.


.. image:: images/texture-packer-editor/atlas-file-wizard.png
   :alt: Atlas file wizard.



Add images to the atlas
-----------------------

To add more images to the atlas, drag the files from the `Project view <workbench.html#project-view>`_ and drop them into Texture Packer Editor. You can drop folders too, the editor will scan for the images and add them.

.. image:: images/texture-packer-editor/drop-images-1.png

Delete images from the atlas
----------------------------



Build atlas
-----------