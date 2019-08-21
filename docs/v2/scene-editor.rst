.. include:: _header.rst
   
.. sectnum::
   :depth: 3
   :start: 7

Scene Editor
============

The |SceneEditor|_ is the most important editor of the IDE. As its name suggests, this editor provides visual tools to create scenes (or levels).

.. image:: images/scene-editor/scene-editor-screenshot.png
  :alt: Scene Editor screenshot.

The |SceneEditor|_ is pretty similar to others scene makers in the market, it has common and particular features. Maybe the most relevant difference is that this editor `compiles the scene <#scene-compiler>`_ into readable Phaser_ code.

Phaser_ provides support for certain file formats like the `asset pack <asset-pack-editor.html>`_ and the `sprite animations <animations-editor.html>`_, but it lacks of a full-feature Scene file format suitable for level editors. And maybe it should be that way, because Phaser_ is a framework and you may use it in very different ways and in very different context.

So, how can we develop a scene editor? We know we need to save the scene in a custom format. We have two main options:

#. We can create a custom runtime, or plugin, to load the scene files in the game and create the objects in the fly.

#. We can create a scene compiler, that translate the custom scene file into Phaser_ code.

The second option plays much better with the |PhaserEditor|_ philosophy of be full compatible with a vanilla Phaser_ runtime. So this compiler gets a custom scene file and generates a clean, readable, hand-writing-like Phaser_ code. Even this option has other advantages: it is very easy to debug the scene and know exactly how the objects are created.


Create a new Scene file
-----------------------

Adding objects to the scene
---------------------------

Supported object types
----------------------

Common properties
~~~~~~~~~~~~~~~~~

Variable properties section
###########################

Editor properties section
#########################

Game Object property section
############################

Transform properties section
############################

Origin properties section
#########################

Flip properties section
#######################

Scroll Factor properties section
################################

Tint properties section
#######################



Game Object
~~~~~~~~~~~



Image
~~~~~



Sprite
~~~~~~

Tile Sprite
~~~~~~~~~~~

Bitmap Text
~~~~~~~~~~~

Text
~~~~

Group
~~~~~

Scene Compiler
--------------


Scene Editor preferences
------------------------


Scene Editor toolbar
--------------------


Blocks view
-----------


Outline view
------------


Keyboard shortcuts
------------------


