Workbench Overview
==================

Phaser Editor is based on the Eclipse IDE and inherites its concepts and features. In this chapter we explain some key concepts that are common to modern editors and IDEs but could be different in Phaser Editor.

The Eclipse IDE is a general purpose tool that is open, flexible and powerful. Maybe for this reason, it results complex for some users, but we belive that we can customize and transform the Eclipse IDE into a friendly and productive tool for game development.

When you run the editor, it opens the workbench in the selected workspace. The workbench contains windows, and each window contains parts (views and editors), an editor area, main toolbar and main menu. All these elements are grouped and layout in a perspective, and you can switch from one perspective to other. Different windows may contain different perspectives. For example, you can open the Scene design perspective in a window and the Phaser Labs perspective in other window, that you can move to a second monitor.


.. image:: images/workbench-overview.png

`Learn more about the workbench in the Eclipse Help <https://help.eclipse.org/2019-06/topic/org.eclipse.platform.doc.user/gettingStarted/qs-02a.htm?cp=0_1_0_0>`_

Views
-----

A view is a small window, or better say, a **part**, inside the workbench window. They are commonly used to present the information of certain resource (workspace, project or file). A view may have a menu or/and a toolbar, with commands that only affects the view's content.

.. image:: images/view-menu-toolbar.png

Most of the view are about to navigate content or show the properties of an object. However, some views allow to edit content, but that content is modified at the moment, there is not the **dirty** concept available in editors. A view may persists its state in the workspace metadata.

You can add, close, stack, dock, minimize/maximize views. The views layout are part of the perspective and is persisted across sessions or perspective switching.

`Learn more about views in the Eclipse Help <https://help.eclipse.org/2019-06/topic/org.eclipse.platform.doc.user/gettingStarted/qs-02e.htm?cp=0_1_0_1_1>`_

Editors
-------

The editors are, like the views, **parts** of the workbench window. You can close, add, stack, dock, minimize/maximize editors, but the editor layout are not part of the perspective. This means, when you switch to other perspective, the editors remains open. Only the editor area is affected.

Editors have input. The common input of an editor is a file. The editors have a **dirty** state, that is activated when the content is modified but not saved. When you close an editor it shows a confirmation dialog if its state is **dirty**.

An editor can contribute items to the `main toolbar <#the-main-toolbar>`_. When an editor is activated, the center of the toolbar is filled with its contributions.

.. image:: images/workbench/toolbar-contributions.png

`Learn more about the editors in the Eclipse Help <https://help.eclipse.org/2019-06/topic/org.eclipse.platform.doc.user/gettingStarted/qs-02d.xhtml?cp=0_1_0_1_0>`_

Perspectives
------------

A perspective groups views and menu items with a common purpose. For example, the `Scene perspective <#scene>`_ provides the views to better design scenes and related assets.

The layout of the views and the editor area is persisted in the perspective. If you change the layout, the perspective is modified. Actually, you can reset a perspective or save its layout as a new perspective. 

In the `Window → Perspective` menu is listed the operations you can do with perspectives (Open, Customize, Save As, Reset, Close, Close All). In addition, in the toolbar you can do a few of them:

.. image:: images/toolbar-perspective-section.png

1. Open a view.
2. Reset the perspective.
3. Switch to other perspective.

`Learn more about perspectives in the Eclipse Help <https://help.eclipse.org/2019-06/topic/org.eclipse.platform.doc.user/gettingStarted/qs-43.htm?cp=0_1_0_15>`_


Workspace and projects
----------------------

The workspace if a folder that stores the projects and metadata (like preferences or any other data needed by the plugins). When you run the editor, it first `opens the launcher <first-steps.html#run-phaser-editor-2d>`_, to select the workspace folder. Then, the workbench is opened and presents all the data of the selected workspace.

Usually, to develop a game you only need a project. The Eclipse IDE supports references between projects but it is something that you don't really need for Phaser development. A common setup is to create a workspace for each game, with the main game project game and maybe other projects related to the game: to test or learn some Phaser features or develop ideas related to the game.

To simplify the workflow, Phaser Editor uses the concept of **active project**. The idea is to provide a group of views, editors and menus that are attached to the **active project**.

`Learn more about the active project <first-steps.html#switching-projects>`_

Import wizard
=============

`Learn more about the workspace in the Eclipse Help <TODO.todo>`_



Preferences and themes
----------------------


`Learn more about preferences in the Eclipse Help <TODO.todo>`_

Offline help
------------

TODO

The main toolbar
----------------

TODO

Properties view
---------------


Outline view
------------


Blocks view
-----------


Projects view
-------------


Terminal view
-------------


Main perspectives
-----------------


Start perspective
~~~~~~~~~~~~~~~~~


Scene perspective
~~~~~~~~~~~~~~~~~


Git perspective
~~~~~~~~~~~~~~~


Update
------

`Learn more about the update system in the Eclipse Help <TODO.todo>`_