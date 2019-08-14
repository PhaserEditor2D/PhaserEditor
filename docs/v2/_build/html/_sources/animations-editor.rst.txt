.. include:: _header.rst
   
.. sectnum::
   :depth: 3
   :start: 5

.. highlight:: javascript

Animations Editor
=================

Probably, the most common animations in Phaser_ games are the so called sprite-animations or frame-based animations.

Animation:

.. image:: images/animations-editor/eagle.gif
  :alt: Eagle animation.

Animation frames:

.. image:: images/animations-editor/eagle-spritesheet.png
  :alt: Eagle animation frames. 


In Phaser_ v3, the animations are `created <https://photonstorm.github.io/phaser3-docs/Phaser.Animations.AnimationManager.html#create__anchor>`_ as global objects, in the `animations manager <https://photonstorm.github.io/phaser3-docs/Phaser.Animations.AnimationManager.html>`_:

You can create a single animation:

.. code::

    this.anims.create({
        "key": "acorn",
        "frameRate": 12,
        "repeat": -1,
        "frames": [
            {
            "key": "atlas",
            "frame": "acorn-1"
            },
            {
            "key": "atlas",
            "frame": "acorn-2"
            },
            {
            "key": "atlas",
            "frame": "acorn-3"
            }
        ]
    });

Or multiple animations:

.. code::

    this.anims.fromJSON(
        "anims": [
            { 
                "key": "acorn", 
                // ....
            },
            { 
                "key": "player", 
                // ....
            }
        ]
    );

The common is to create the animations once in the game, probably in the preloader scene. Later, you can play an animation on a sprite object passing the animation key to the `play(..) <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.Sprite.html#play__anchor>`_ method:

.. code::

    mySprite.play("acorn");

Other way to create the animations is packing them all in a single JSON file, and load the file using the `this.load.animation(..) <https://photonstorm.github.io/phaser3-docs/Phaser.Loader.LoaderPlugin.html#animation__anchor>`_ method:

.. code::

    this.load.animations("my-anims", "assets/animations.json");

|PhaserEditor|_ provides the |AnimationsEditor|_, to create the animations JSON file. So, the workflow is very simple:


* Create the animations JSON file with the |AnimationsEditor|_.

* Import the animations JSON file into an **Asset Pack** file with the |AssetPackEditor|_.

* Play the animations in your code, with the `play(..)`_ method.


Create the animations file
--------------------------

The default `project template <workbench.html#project-structure>`_ contains an animations file, but you can create other animations file with the *File* |-| *New* |-| *Animations File* menu option, or pressing the `New <workbench.html#new-button>`_ button of the main toolbar and selecting the *Animations File* option.

.. image:: images/animations-editor/animations-new-file.png
  :alt: New Animations File.

Adding animations to the file
-----------------------------

To create a new, empty animation, press the **Add Animation** button in the toolbar. It shows a dialog to enter the animation name.


.. image:: images/animations-editor/animations-new-empty.png
  :alt: Create a new, empty animation.

The new animation is empty, so you have to add some frames to it. The frames (or better said, the frame keys) could be dragged from the `Assets view <workbench.html#assets-view>`_ or the `Blocks view <workbench.html#blocks-view>`_ and dropped into the animation timeline.


.. image:: images/animations-editor/animations-drop-frames-timeline.png
  :alt: Drop frames to the timeline.

.. image:: images/animations-editor/animations-added-frames.png
  :alt: Frames added to the animation.

Automatic creation of animations
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~




Animation playback buttons
--------------------------


Properties view
---------------


Outline view
------------