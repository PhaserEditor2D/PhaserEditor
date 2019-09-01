.. include:: _header.rst
   
.. sectnum::
   :depth: 3
   :start: 7

Scene Editor
============

The |SceneEditor|_ is the most important editor of the IDE. As its name suggests, this editor provides visual tools to create scenes (or levels).

.. image:: images/scene-editor/scene-editor-screenshot.png
  :alt: Scene Editor screenshot.

The |SceneEditor|_ is pretty similar to other scene makers in the market, it has common and particular features. Maybe the most relevant difference is that this editor `compiles the scene <#scene-compiler>`_ into readable Phaser_ code.

Phaser_ provides support for certain file formats like the `asset pack <asset-pack-editor.html>`_ and the `sprite animations <animations-editor.html>`_, but it lacks a full-feature Scene file format suitable for level editors. And maybe it should be that way because Phaser_ is a framework and you may use it in very different ways and very different context.

So, how can we develop a scene editor? We know we need to save the scene in a custom format. We have two main options:

#. We can create a custom runtime, or plugin, to load the scene files in the game and create the objects in the fly.

#. We can create a scene compiler, that translates the custom scene file into Phaser_ code.

The second option plays much better with the |PhaserEditor|_ philosophy of being fully compatible with a vanilla Phaser_ runtime. So this compiler gets a custom scene file and generates a clean, readable, hand-writing-like Phaser_ code. Even this option has other advantages: it is very easy to debug the scene and know exactly how the objects are created.

In the previous version of |PhaserEditor|_, we used custom scene renderers based on desktop UI toolkits, because of the lack of a **Web View** available in all the OS platforms. However, now the |EclipseIDE|_ provides better support for its **Web View** and we migrated the scene renderer to Phaser_. This means, what you see in the scene editor is what you get in the game because both are built with the same technology: Phaser_.


Create a new Scene file
-----------------------

The scene files (``*.scene``) are created using the **New Scene File** wizard. This wizard can be opened from the main menu: *File* |-| *New* |-| *Scene File*, or using the `New button <workbench.html#new-button>`_.

.. image:: images/scene-editor/scene-editor-new-file.png
  :alt: New Scene File.

The **New Scene File** wizard is divided into two steps:

#. Set the location and name of the new scene file.

#. Select an `asset pack file <asset-pack-editor.html#importing-javascript-files>`_ to import the scene compiled code in the game. This is optional.

.. image:: images/scene-editor/scene-editor-new-wizard-1.png
  :alt: New Scene File wizard, page 1.

.. image:: images/scene-editor/scene-editor-new-wizard-2.png
  :alt: New Scene File wizard, page 2.

Adding objects to the scene
---------------------------

When you activate the |SceneEditor| (open, select, focus on) the `main toolbar <workbench.html#main-toolbar>`_ shows a couple of buttons dedicated to the |SceneEditor|_. In the middle, there is the **Add Object** button.

The **Add Object** button shows a menu with the object types supported by the |SceneEditor|_.

.. image:: images/scene-editor/scene-editor-add-button.png
  :alt: The Add Object button.

Some objects typically use a resource. The `Image`_, `Sprite`_ and `Tile Sprite`_ types use textures (images, atlas frames, sprite-sheet frames). The `Bitmap Text`_ uses a bitmap font. When you select to add any of these objects, the editor opens a dialog to select the asset needed by the object.

.. image:: images/scene-editor/scene-editor-add-object-select-texture.png
  :alt: Select the texture of a new object.

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
Blocks element          Scene Object type
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

In the same way, you `drop file keys from the Blocks view <adding-objects-from-the-blocks-view>`_ into the scene, you can drag the file keys from the |AssetsView|_ and drop them into the scene. Take a look at the previous section for more details on the kind of objects are created.

.. image:: images/scene-editor/scene-editor-drag-from-assets-view.png
  :alt: Drag file keys from the Assets view.

Supported object types
----------------------

The |SceneEditor|_ is in active development and only supports a very basic set of object types. Eventually, we should add more types and more properties. However, you can use an `object factory <#making-reusable-objects>`_ to create objects of any type, even your custom type.

The list of supported object types is:

* `Image`_

* `Sprite`_

* `Tile Sprite`_

* `Bitmap Text`_

* `Text`_

* `Group`_


Common properties
~~~~~~~~~~~~~~~~~

Like it is in the other editors of the IDE, the `Properties view <workbench.html#properties-view>`_ connects with the active |SceneEditor|_ and show the properties of the objects selected in the scene or the `Outline view`_.

In the Phaser_ API, the properties of the game object types are divided into dedicated classes in the `Phaser.GameObjects.Components <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.Components.html>`_ namespace. So, an object type contains its properties and methods but also inherits a couple of "component" classes, like `Phaser.GameObjects.Components.Transform <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.Components.Transform.html>`_ or `Phaser.GameObjects.Components.Tint <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.Components.Tint.html>`_.
 

In the `Properties view`_ of the |SceneEditor| we split the properties more or less in the same way, each property section is dedicated to a specific Phaser_ game object component class. There is not a 100% match, there are sections dedicated only to |SceneEditor|_-properties or there are sections with a merge of Phaser_ API properties and editor-properties.

The editor-properties are not part of the Phaser_ API and are needed by the editor to help create the scenes, but are not generated in the final code. They are design-time properties.

In this chapter, we explain the properties that are common to the majority of the supported game object types.

Variable property section
###########################

This section is used by the editor and the `scene compiler`_ to identify the object and provides certain operations.

.. image:: images/scene-editor/scene-editor-variable-section.png
  :alt: Variable section.


#. The **generate property** button. You can check or un-check it. By default, the `scene compiler`_ assign an object to a local variable, but if you set true this property, the object is assigned to an instance field. Do this when you need to get access to the object outside the *create* method.

#. The **go-to source** button. Click it to open the JavaScript editor and scroll to the line where this object is created. If you enabled an `external editor <code-editors.html#integration-with-external-editors>`_ then it opens the external editor.

#. The **Var Name** property is used by the `scene compiler`_ as the variable name and the `Outline view`_ use it as the label of the object.
  
  .. image:: images/scene-editor/scene-editor-variable-section-var-name-property.png
    :alt: Variable name.

4. The same of *(1)*.

Editor property section
#########################

A section that contains properties and buttons that are not part of the Phaser_ API but are used to provide certain functionalities of the editor.

.. image:: images/scene-editor/scene-editor-editor-section.png
  :alt: Editor section.


1. The **Type** property displays the `type of the game object <#supported-object-types>`_. You can click the button `to morph the object to another type <#morphing-objects-to-a-different-type>`_, which is an important feature.

  .. image:: images/scene-editor/scene-editor-editor-section-type-property.png
    :alt: Type property.

2. The **Transparency** property. You can change its value to render the object in the editor with certain transparency, but it is not applied to the object in the game. Note it is not included in the generated code.

  .. image:: images/scene-editor/scene-editor-editor-section-transparency-property.png
    :alt: Transparency property.

3. The **Order** buttons allow changing the order the objects are created in the scene, which is the same order they are rendered.

  .. image:: images/scene-editor/scene-editor-editor-section-order.png
    :alt: The Order buttons.

  1. Moves the object up in the order.

  2. Moves the object down in the order.

  3. Moves the object to the top of the order.

  4. Moves the object to the bottom of the order.

  You can see in the `Outline view`_ the result of the ordering.

4. The **Groups** displays the groups that contain the object, and buttons to add or remove the object to the groups. A group (that is part of the Phaser_ API, see `Phaser.Group <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.Group.html>`_ ) is a powerful resource in your hands. You can use it as an object pool or as an object classifier. In many Phaser_ examples and games, you can see how the groups are used to apply common behavior to objects instead of creating custom types. `Read more about groups <#group>`_.

  .. image:: images/scene-editor/scene-editor-editor-section-group.png
    :alt: Groups property.

  1. Lists the groups containing the object. One object could be added to many groups.    

  2. A button to add the object to an existent group, or a new group.

    .. image:: images/scene-editor/scene-editor-editor-section-add-group-button.png
      :alt: Add to group button.

  3. A button to delete the object from one of its groups.

    .. image:: images/scene-editor/scene-editor-editor-section-remove-from-group-button.png
      :alt: Removes an object from a group.

  4. A button to select a specific group and see its content.

    .. image:: images/scene-editor/scene-editor-editor-section-select-group-button.png
      :alt: Select group.

5. The **Snapping** button is a shortcut to set the snapping value of the scene editor, using the size of the selected object. Many games use fixed-size images and the objects are placed in a grid. The snapping feature of the |SceneEditor|_ helps to set a custom grid and place the objects quickly. See more in the `Snapping property section`_.

Game Object property section
############################

The majority of the objects you add to the scene are a subtype of the `Phaser.GameObjects.GameObject <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.GameObject.html>`_. The **Game Object** section exposes properties of the `GameObject <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.GameObject.html>`_ class and other properties that are not part of the Phaser_ API but are used by the editor to provide more flexibility.

.. image:: images/scene-editor/scene-editor-game-object-section.png
  :alt: Game Object section.


1. The **Active** button. It sets On or Off the `active <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.GameObject.html#active__anchor>`_ property.

2. The **Visible** button. It sets On or Off the `visible <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.Components.Visible.html#visible__anchor>`_ property. If this button is set On, the object will be rendered in the editor with certain transparency.

3. The **Name** property. If it is selected, then the **Var Name** of the object will be used as `the name <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.GameObject.html#name__anchor>`_ too.

4. The **Factory** property is not Phaser_ API, it says to the `scene compiler`_ to use a different factory for the object. You can write the new factory name directly in the text-box or select one from the menu. Leaves it empty to use the default factory.

  .. code::

    // using the default factory "sprite"
    var gator = this.add.sprite(560.0, 144.0, "atlas", "gator-1"); 

    // using the custom factory "gator"
    var gator = this.add.gator(560.0, 144.0, "atlas", "gator-1");


  .. image:: images/scene-editor/scene-editor-game-object-factory.png
    :alt: Game Object factory.

  Learn more about factories in the `Making reusable objects`_ section.

5. The **Factory R. Type** property is not Phaser_ API, you can use it together with the **Factory** property to create custom objects. When you use a custom factory, probably, the object returned by it is a custom type too, like ``Player`` or ``Enemy``. You can use this property to set the return type of a custom factory. The variables that reference the object will be declared with the custom type and you will get access to all its properties and methods.

  In JavaScript, the code is generated like this:

  .. code::

    var gator = this.add.gator(560.0, 144.0, "atlas", "gator-1");
    /** @type {Gator} */
    this.fGator = gator;
    

  Note that, if an instance property is generated for the object (``fGator``), then it is annotated with the ``Gator`` type.

  In TypeScript, it has even more sense, because this language supports static typing. In the above case, it will generated a property with the ``Gator`` type. 

  
  .. code::

    private fGator: Gator;


  Learn more about factories in the `Making reusable objects`_ section.

6. The **Build Object** property is not part of the Phaser_ API. It indicates to the `scene compiler`_ to generate a call to the ``build`` method. This is useful for objects that are created with a custom factory and a custom object type. You can implement a ``build`` method in the custom type to setup the object with initial values that may depend on other object properties. Look in the next image how in the ``build`` method the velocity values are set regarding the user data set to the object.

  .. image:: images/scene-editor/scene-editor-game-object-build.png
    :alt: Game Object build.

7. The **Data** property is associated to the `data <https://photonstorm.github.io/phaser3-docs/Phaser.GameObjects.GameObject.html#setData__anchor>`_ property of the Phaser_ API. This property is a reference to the `Phaser.Data.DataManager <https://photonstorm.github.io/phaser3-docs/Phaser.Data.DataManager.html>`_. It allows to set user data to the object. 

  This is the workflow:

  1. Click in the **add** button and write the new data property name.

    .. image:: images/scene-editor/scene-editor-game-object-data-add.png
      :alt: Add data property.

  2. The property was added, now write its value. This value is verbatim-written in the generated code. This means, you can write literals like ``"high"``, ``{value:10, dir:"left"}`` or even JavaScript expressions like ``Math.random() * gator.width``.

    .. image:: images/scene-editor/scene-editor-game-object-data-value.png
      :alt: Set the data property value.

  This is how it is inserted in the generated code:

  .. code-block:: javascript
    :emphasize-lines: 4,4

    var gator = this.add.gator(576.0, 368.0, "atlas", "gator-1");
    gator.setData("distance", 40);
    gator.setData("horizontal", false);
    gator.setData("power", 10);
    gator.build();

  3. Delete or select other objects with the same property or the same property and value.

    .. image:: images/scene-editor/scene-editor-game-object-data-value-menu.png
      :alt: Data property menu.

Transform property section
############################

Origin property section
#########################

Flip property section
#######################

Scroll Factor property section
################################

Tint property section
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


Making reusable objects
~~~~~~~~~~~~~~~~~~~~~~~


Scene properties
----------------

Snapping property section
~~~~~~~~~~~~~~~~~~~~~~~~~

Display property section
~~~~~~~~~~~~~~~~~~~~~~~~

Compiler property section
~~~~~~~~~~~~~~~~~~~~~~~~~

WebView property section
~~~~~~~~~~~~~~~~~~~~~~~~


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


