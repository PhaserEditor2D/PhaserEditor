.. include:: _header.rst

.. sectnum::
 :depth: 3
 :start: 4

.. highlight:: javascript

Asset Pack Editor
=================

In a Phaser_ game you load the files using the methods of the `Phaser.Loader.LoaderPlugin <https://photonstorm.github.io/phaser3-docs/Phaser.Loader.LoaderPlugin.html>`_ class. This is how you can `load a sprite-sheet <https://photonstorm.github.io/phaser3-docs/Phaser.Loader.LoaderPlugin.html#spritesheet__anchor>`_ file:


.. code::

  this.load.spritesheet('bot', 'images/robot.png', { frameWidth: 32, frameHeight: 38 });

You pass a key (``'bot'``) to identify the file in the `game cache <https://photonstorm.github.io/phaser3-docs/Phaser.Cache.CacheManager.html>`_, the URL of the file (``'images/robot.png'``) and a sprite-sheet configuration object, with other information like the frame size.

Or you can load the file by passing a single argument, a `SpriteSheetFileConfig <https://photonstorm.github.io/phaser3-docs/Phaser.Types.Loader.FileTypes.html#.SpriteSheetFileConfig__anchor>`_ configuration object:

.. code::

 this.load.spritesheet({
    key: 'bot',
    url: 'images/robot.png',
    frameConfig: {
    frameWidth: 32,
    frameHeight: 38
    }
 });


Every file type can be loaded using its configuration object, that is just a JSON object. Following this logic, Phaser_ has an especial type of files that contains the configurations of other files, it is the `Asset Pack File <https://photonstorm.github.io/phaser3-docs/Phaser.Types.Loader.FileTypes.html#Phaser.Loader.LoaderPlugin.html#pack__anchor>`_.


The **Asset Pack** files are loaded this way:

.. code::

 this.load.pack("assets/pack.json");


|PhaserEditor|_ provides an editor for the **Asset Pack** files, making it very easy to load the assets in your game. Instead of spending a precious amount of time writing the file configurations, with the **Asset Pack Editor** you can import the files with a visual tool and semi-automatic wizards.

.. image:: images/asset-pack-editor/asset-pack-editor.png
  :alt: Asset Pack Editor.


The relevance of the Asset Pack File
------------------------------------

The **Asset Pack File** is relevant for two main reasons:

* It is a Phaser_ built-in format. This means, you can create **Asset Pack** files with |PhaserEditor|_ and use them in any Phaser_ project, inside or outside |PhaserEditor|_. 
* In the same way Phaser_ uses keys to represent the real files, many |PhaserEditor|_ tools use the files configured in the **Asset Pack** file as resource references. For example, a sprite object in the |SceneEditor|_ does not have a reference to a real image, else it uses a file key that, at rendering time, is used to find the real image under that key in any of the **Asset Pack** files of the project.

Create a new Asset Pack file
----------------------------

The default project created by the `Project wizard <workbench.html#phaser-project-wizard>`_ contains a pack file, however, you may want to create other pack files with a different purpose.

To create a new **Asset Pack** file: 

* Click on *File* |-| *New* |-| *Asset Pack File*.
* Or click on the main toolbar's `New <workbench.html#new-button>`_ button, and select **Asset Pack File**.


Adding file configurations
--------------------------

You can add new file configurations in two ways:

* By pressing the **Add File Key** in the main toolbar.

* By importing files from the Blocks view.

Adding files with the Add File Key button
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

These are the steps:

#. Press the **Add File Key** button of the toolbar.
 
 .. image:: images/asset-pack-editor/add-new-file-button.png
   :alt: Add New File Key button.

#. It opens a dialog with all the file types, select the type of file you want to add.

 .. image:: images/asset-pack-editor/select-file-type-dialog.png
   :alt: Select the file type.

#. When you select a file type, it opens a dialog with a list of files selected following rules. Select the files you want to import.

 * The files belong to the folder, or sub-folder, of the Pack File. It is not a restriction of the Pack Files, but we use it to simplify the process of import the files. 

 * The content type or extension of the files are compatible with the type selected. For example, if you select to add an Image, then only image files are shown.

 * Files that are not used by any pack file in the project are highlighted (in bold).


 .. image:: images/asset-pack-editor/select-files-to-import-dialog.png
   :alt: Files dialog.

#. Change the properties of the file configurations in the `Properties view <workbench.html#properties-view>`_.


 .. image:: images/asset-pack-editor/file-edit-properties.png
   :alt: Edit properties of a file configuration.

The context menu also has the option **Add File Key**.


Importing files from the Blocks view
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The `Blocks view <workbench.html#blocks-view>`_ is a general-purpose view that connects with the active editor and provides the "blocks" needed to build the objects of the editor.

In the case of the **Asset Pack Editor**, the `Blocks view`_ shows the files that are candidates to be imported. A file is a candidate to be imported if:

* The file belongs to the folder, or sub-folder, of the Asset Pack file of the editor.

* The file is not present in any other pack file of the project.

* If the file has a content type or file name extension that we know is never loaded in games: 

 * TypeScript files (``.ts``).

 * |TexturePackerEditor|_ files (``.atlas``).

 * |SceneEditor|_ files (``.scene``).

 * Other **Asset Pack** files.

The workflow is the following:

#. Select the files to be imported in the `Blocks view`_.

#. Select one of the options listed in the `Properties view`_ to import the files.

 .. image:: images/asset-pack-editor/import-files-from-blocks-view.png
   :alt: Import files from Blocks view.

#. Edit the new file configurations in the `Properties view`_.

This is a shorter workflow, you select the files you want to import and the editor guesses automatically what type of configuration needs to be created.

This process to guess the type of files provides three groups of options:

#. Guess the type of the file from its content type. It is the case of atlas files, animations files, bitmap files, tilemap files, image and audio files, JavaScript files associated with scenes, audio-sprites files.

#. Guess the type of the file just by its extension. For example, ``.json`` and ``.xml``.

#. The last option is not associated with any file type, it opens a dialog with all the file types and you should select the type you consider is the indicated from the selected files.

.. image:: images/asset-pack-editor/import-files-from-blocks-view-options.png
  :alt: The different options to import the files.

Importing JavaScript files
--------------------------

In the JavaScript development world, there are multiple ways to load the script files. The common is to load them using ``<script>`` tags in the ``index.html`` file. Some frameworks allow loading script files at any time, via code. Phaser_ provides different ways to load the scripts, each one with its purpose:

* `this.load.script(...) <https://photonstorm.github.io/phaser3-docs/Phaser.Loader.LoaderPlugin.html#script__anchor>`_: it load and execute the provided script files.

* `this.load.scripts(...) <https://photonstorm.github.io/phaser3-docs/Phaser.Loader.LoaderPlugin.html#scripts__anchor>`_: it loads a list of script files and execute them in the same order. Note in the ``script()`` method the files may be executed in random order.

* `this.load.scenePlugin(...) <https://photonstorm.github.io/phaser3-docs/Phaser.Loader.LoaderPlugin.html#scenePlugin__anchor>`_: it loads the script files and execute them, but assume them create new `Phaser.Scenes.ScenePlugin <https://photonstorm.github.io/phaser3-docs/Phaser.Loader.LoaderPlugin.html#Phaser.Scenes.ScenePlugin.html>`_ instances.

* `this.load.sceneFile(...) <https://photonstorm.github.io/phaser3-docs/Phaser.Loader.LoaderPlugin.html#sceneFile__anchor>`_: it loads the script files and execute them, but assume them create `Phaser.Scene <https://photonstorm.github.io/phaser3-docs/Phaser.Loader.LoaderPlugin.html#Phaser.Scene.html>`_ instances.

So the same Phaser_ framework can be used as a JavaScript packing/loading tool, and it has some clear advantages:

* You don't need a third-party tool to control the scripts loading.

* You can report loading progress of the script files just like you do with the other assets.

* The scripts can be added to the **Asset Pack** files using the |PhaserEditor|_ toolset.

By the way, when you add a JavaScript file to an **Asset Pack** file, and that script is associated with a |SceneEditor|_ file (``.scene``), then the **Asset Pack Editor** shows a screenshot of the scene, as file icon, for easy identification.

.. image:: images/asset-pack-editor/asset-pack-scene-scripts.png
  :alt: Scene JavaScript files are displayed with a scene screenshot.


Organizing the Asset Pack files
-------------------------------

You can place **Asset Pack** files in any folder inside the ``WebContent`` folder. However, we recommend placing these files in the folders dedicated to the game assets, for example: ``WebContent/assets/pack.json``.

The common is that you need more than one **Asset Pack** file, at least, one for the preload screen and other for the rest of the game screens.

Sometimes, you need to use "helper" assets in the |SceneEditor|_, to create custom objects. You can group all these assets with its own **Asset Pack** file in a separated folder. The "helper" assets are only for design purpose, as references in the |SceneEditor|_, so you don't need to load them in the game and you should exclude them from the distribution build.

This could be a structure of your project:

.. code::

    WebContent/
      assets/
        preload/
          preload-pack.json
          // preload assets ...
        levels/
          levels-pack.json
          // level assets ...
        helpers/
          helper-pack.json
          // helper assets ...

Remember the Asset Pack Editor searches for files inside the folder or sub-folder of the **Asset Pack** file, and you can make it more effective if the structure of the project is well organized.

And remember the `gold rule <workbench.html#resource-filters>`_: don't add to your project (or filter them off) files that are not used by the game or any tool of the |PhaserEditor|_. For example, server-side ``node_modules`` or ``.git`` folders may pollute and slow down all the |PhaserEditor|_ experience.


Outline view
------------

The general purpose `Outline view <workbench.html#outline-view>`_ connects with the active **Asset Pack Editor** and shows all the file configurations grouped by its type. In case of complex files, like atlas or sprite-sheet files, it shows the frames too.

.. image:: images/asset-pack-editor/asset-pack-outline-view.png
  :alt: Outline view.


Common operations
-----------------

All operations like add, delete, modify the file configurations can be undone and redone. To delete file keys you can press the ``Delete`` key or use the context menu.



Asset Pack state of the project
-------------------------------

As we mentioned at the beginning of this chapter, the files configured in the **Asset Pack** files are used by other tools in |PhaserEditor|_. For these reasons, the editor keeps an internal, in-memory model (or cache) of the file configurations, so it is not required to parse all the **Asset Pack** files each time a tool needs the information about the files.

This in-memory state is computed by project builders that run each time a file is modified, and it is possible that something (like out of synchronization resources) breaks the builders or that other builders (like thumbnail builders) that run asynchronous operations are not done at a certain moment. In these cases, you can `clean the state of the project <workbench.html#cleaning-projects>`_ and run the builders again.

You can see the in-memory **Asset Pack** files state in the `Assets view <workbench.html#assets-view>`_.

.. image:: images/workbench/assets-view.png
  :alt: The Assets view.