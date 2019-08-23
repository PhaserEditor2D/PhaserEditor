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

In previous version of |PhaserEditor|_, we used custom scene renderers based on desktop UI toolkits, because the lack of a **Web View** available in all the OS platforms. However, now the |EclipseIDE|_ provides better support for its **Web View** and we migrated the scene renderer to Phaser_. This means, what you see in the scene editor is what you get in the game, because both are built with the same technology: Phaser_.


Create a new Scene file
-----------------------

The scene files (``*.scene``) are created using the **New Scene File** wizard. This wizard can be opened from the main menu: *File* |-| *New* |-| *Scene File*, or using the `New button <workbench.html#new-button>`_.

.. image:: images/scene-editor/scene-editor-new-file.png
  :alt: New Scene File.

The **New Scene File** wizard is divided in two steps:

#. Set the location and name of the new scene file.

#. Select an `asset pack file <asset-pack-editor.html#importing-javascript-files>`_ to import the scene compiled code in the game. This is optional.

.. image:: images/scene-editor/scene-editor-new-wizard-1.png
  :alt: New Scene File wizard, page 1.

.. image:: images/scene-editor/scene-editor-new-wizard-2.png
  :alt: New Scene File wizard, page 2.

Adding objects to the scene
---------------------------

When you activate the |SceneEditor| (open, select, focus on) the `main toolbar <workbench.html#main-toolbar>`_ shows a couple of buttons dedicated to the |SceneEditor|_. In the middle there is the **Add Object** button.

The **Add Object** button shows a menu with the object types supported by the |SceneEditor|_.

.. image:: images/scene-editor/scene-editor-add-button.png
  :alt: The Add Object button.

There are objects that typically uses a resource. The `Image`_, `Sprite`_ and `Tile Sprite`_ types use textures (images, atlas frames, sprite-sheet frames). The `Bitmap Text`_ uses a bitmap font. When you select to add any of these objects, the editor opens a dialog to select the asset needed by the object.

.. image:: images/scene-editor/scene-editor-add-object-select-texture.png
  :alt: Select texture of new object.

Finally, the new object is added to the center of the scene:

.. image:: images/scene-editor/scene-editor-add-object-done.png
  :alt: Added object.

When you select to add a `Text`_ object, it does not open any dialog because it does not require any asset, the `Text`_ object is created with a ``New Text`` value and added to the center of the scene.

.. image:: images/scene-editor/scene-editor-add-text.png
  :alt: Added a Text object.


Adding objects from the Blocks view
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When the `Blocks view`_ connects with the active |SceneEditor|_, it shows all the `file keys or file configurations <asset-pack-editor.html>`_ that you can use to create new objects.

A new object is created when you drag a file key from the `Blocks view`_ and drop it into the scene.

Each type of file creates a particular type of object by default. However, once the object is created, `you can morph it to another type <#morphing-objects-to-a-different-type>`_.

====================== ====================
Block element          Scene Object type
====================== ====================
Image key              `Image`_
Atlas frame key        `Image`_
Sprite-sheet frame key `Image`_
Animation key          `Sprite`_
Bitmap Font key        `Bitmap Text`_
====================== ====================

.. image:: images/scene-editor/scene-editor-drag-from-blocks-view.png
  :alt: Dropping an animation from the Blocks view.

.. image:: images/scene-editor/scene-editor-drag-from-blocks-view-done.png
  :alt: The new object is created with a Sprite type.

Adding objects from the Assets view
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The |AssetsView|_ shows the files created by the |PhaserEditor|_ editors. In the case of the `Asset Pack files <asset-pack-editor.html>`_, it also shows the file keys defined in the file.

In the same way you `drop file keys from the Blocks view <adding-objects-from-the-blocks-view>`_ into the scene, you can drag the file keys from the |AssetsView|_ and drop them into the scene. Take a look to the previous section for more details on the kind of objects are created.

.. image:: images/scene-editor/scene-editor-drag-from-assets-view.png
  :alt: Drag file keys from the Assets view.

Supported object types
----------------------

The |SceneEditor|_ is in active development and only supports a very basic set of object types. Eventually we should add more types and more properties. However, you can use an `object factory <#game-object-property-section>`_ to create objects of any type, even of your own custom type.

The list of supported object types is:

* `Image`_

* `Sprite`_

* `Tile Sprite`_

* `Bitmap Text`_

* `Text`_

* `Group`_


Common properties
~~~~~~~~~~~~~~~~~

Like it is in the other editors of the IDE, the `Properties view`_ connects with the active |SceneEditor|_ and show the properties of the objects selected in the scene or the `Outline view`_.

In the Phaser_ API, the properties of the game object types are grouped in small classes, so a game object type extends...

All object types (excepting `Group`_) share a common set of properties that are presented in the `Properties view`_ in sections. 

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

Morphing objects to a different type
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Scene Compiler
--------------


Scene Editor preferences
------------------------

Scene renderer preferences
~~~~~~~~~~~~~~~~~~~~~~~~~~

Scene Editor toolbar
--------------------


Blocks view
-----------

The `general purpose Blocks view <workbench.html#blocks-view>`_ provides building blocks for the active editor. In the case of the |SceneEditor|_, the `Blocks view`_ displays all the `file configurations or file keys <asset-pack-editor.html>`_ that you can use to create the scene objects:

* Image keys.
* Atlas frame keys.
* Sprite-sheet frame keys.
* Animation keys.
* Bitmap Font keys.


Outline view
------------


Keyboard shortcuts
------------------


