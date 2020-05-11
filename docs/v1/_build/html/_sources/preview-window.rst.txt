.. include:: _header.rst

The Preview Window
==================

As the name suggests, in this window you can visualize different objects, especially the asset declarations of the `asset pack <assets-manager.html>`_ and media files (images, sounds, videos).

This Preview window usually shows details of the previewed object like dimension, name, etc... In addition, in the case of textures, you can `drag the frames and drop them into a scene <canvas.html#from-the-preview-window>`_, to create new objects.

To preview an asset you can drag it from the Assets explorer or Project Explorer and drop it into the Preview window, or select an asset and press ``Ctrl+Alt+V``.

In the JavaScript editor, if you put the cursor on a string literal and press ``Ctrl+Alt+V``, the asset of the same name will be opened in the Preview.

.. image:: images/DropAssetPreview.png
	:alt: Drop asset into the preview.



Note in the toolbar of the Preview window there are the following actions:

.. image:: images/PreviewMenu.png


========================= ===============================================
Open New Window           Opens a new Preview, so you can visualize many objects at the same time.
Refresh (``F5``)          To update the content. This is useful when the object was changed by an external tool.
Clear                     Empty the window.
========================= ===============================================


The sprite-sheet preview
~~~~~~~~~~~~~~~~~~~~~~~~

When you open a sprite-sheet asset in the Preview window it shows a special control where you can play animations with the sprite-sheet frames. It is useful for quick animations preview. In addition, you can select a couple of frames and drop them into the scene. To select more than one frame keep the ``Shift`` key pressed and move the mouse over the frames.

.. image:: images/SpritesheetPreviewAnimation.gif
	:alt: You can play animations in the sprite-sheet preview.

The textures atlas preview
~~~~~~~~~~~~~~~~~~~~~~~~~~

The preview of a texture atlas asset has three different modes: Tile, List, and Texture (or Original). The Tile mode shows the sprites of the atlas in a grid that you can zoom in/out (with the mouse wheel). The List mode shows the sprites in a list together with the names, and you can filter them by writing in a search field. The Texture mode shows the atlas texture as it is. In all the modes you can click on a sprite and drag it anywhere on the screen and drop it in other Preview window or in a scene editor. Note the toolbar of the Preview window shows the buttons to change from one mode to other:

.. image:: images/AtlasPreview.gif
	:alt: Preview a texture map.

Image preview
~~~~~~~~~~~~~

To preview an image asset or file, drop it into the Preview window. You can zoom in/out the image with the mouse wheel or move the image by dragging it with the right button of the mouse. The toolbar contains two actions: Reset Zoom and Fit the image to the window area:

.. image:: images/ImagePreview.png
	:alt: Preview an image.


Bitmap Font preview
~~~~~~~~~~~~~~~~~~~

To preview bitmap font just drop it into the window. By default it shows the name of the font. You can change the text by pressing the ``Set text`` command, it opens a text input dialog.

.. image:: images/BitmapFontPreview.png
	:alt: Preview a bitmap text.

CSV Tilemap preview
~~~~~~~~~~~~~~~~~~~

To preview a CSV tilemap, drag it from the Assets view and drop it in the Preview window. The CSV format does not provide any information about the tile size or tilset image of he tilemap, so you have to enters it manually. By default, the tileset image is simulated by assigning a different color to each tile ID, and the tile size is set to 32x32.

This is how looks a tilemap preview by default:

.. image:: images/TilemapPreviewDefault.png
	:alt: A default rendering of tilemap.

To set the tile size, press the "settings" toolbar icon and set the right values in the format ``<width>x<height>``:

.. image:: images/TilemapPreviewSetSize.png
	:alt: Set the size.

To set the tileset image, press the "image" toolbar icon and select the right image asset:

.. image:: images/TilemapPreviewSetTilesetImage.png
	:alt: Set the tileset image.

The result is the tilemap as it should be generated in the game:

.. image:: images/TilemapPreviewFinal.png
	:alt: The final tilemap.


Indexes selection
^^^^^^^^^^^^^^^^^

A nice feature of the tilemap preview is the indexes selection. Note when you move the mouse over the tilemap it shows a label with the ID of the tile behind it. If you click it, that tile is selected, and the ID added to the set of selected IDs, in the text field at the bottom of the window. If you double click a tile (or press the ``SPACE`` key), all the tiles with the same ID are selected and that ID is added to the selection set:

.. image:: images/TilemapPreviewSelectIndexes.png
	:alt: How to select tilemap indexes (tile IDs).

Note the list of the selected IDs has a JavaScript array like syntax, so it is very easy for you to use it in your code, create a list in the preview window and paste it in your code, or get the list of IDs from your code and paste it in the preview window. 


Audio preview
~~~~~~~~~~~~~

To preview an audio asset or file, drop it into the Preview window. It shows a button to playback the sound:

.. image:: images/SoundPreview.png
	:alt: Preview of a sound file or asset.

Audio-sprites preview
~~~~~~~~~~~~~~~~~~~~~

To preview an audio-sprites asset drop it into the Preview window. the preview control contains a combo with the names of the sprites, a sound player with the whole sound file and a playback button to play the selected sprite in the combo.

.. image:: images/AudioSpritePreview.png
	:alt: Preview of a sound file or asset.

Video preview
~~~~~~~~~~~~~

To preview an video asset or file, drop it into the Preview window. It shows a button to playback the video:

.. image:: images/VideoPreview.png
	:alt: Preview of a sound file or asset.
