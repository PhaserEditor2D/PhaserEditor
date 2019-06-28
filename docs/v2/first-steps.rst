First Steps
===========

In this section we explain step by step some of the common tasks you may do when start with Phaser Editor 2D.

Download and install
--------------------

Binaries are available in the `Downloads <https://phasereditor2d.com/blog/downloads>`_ page of the Phaser Editor's website. Phaser Editor is not an installable application, it is distributed in form of ZIP files that you can uncompress and run. Note there are three different files, one for each supported platform: Windows, macOS and Linux.


Run Phaser Editor 2D
--------------------

Uncompress the ZIP file and double click on the executable file ``PhaserEditor2D.exe``. Well, each platform has a different executable name, you can find more details about your platform in the next sections. Together with the executable there is a script to run the editor in debug mode. This mode is useful to get debug messages or start an instance with a clean state. 

When Phaser Editor starts, it shows a splash screen and later a **Launcher Dialog**. In this dialog you should select the path to the workspace. Usually, the default path is fine (a ``workspace`` subfolder in the current dialog), but you can change it. The workspace is a special folder to store the projects and other data like preferences. `Learn more about the workspace <workbench.html#workbench.html#workspace-and-projects>`_. You can change the list of workspaces and other settings in `Preferences → General → Startup and Shutdown → Workspaces`.

.. image:: images/WorkspaceLauncher.png


Windows
~~~~~~~

To run Phaser Editor on Windows run the file ``PhaserEditor2D.exe``. 

Execute the file ``Debug.bat`` to run the editor in debug mode.

macOS
~~~~~

Phaser Editor is distributed in macOS as an application (``.app`` folder). To run it the first time, you should right-click on the ``Phaser Editor 2D`` application and select ``Open``. This step is needed because the editor is not certified by Apple. The second time, you can run the editor with the a double click.

To run the editor in debug mode, you can execute the ``Debug.sh`` script in a terminal.

When we build the editor for macOS we add execution permission to a series of files, but if the OS shows a message about execution permissions, you can set them again using the ``SetExecPermissions.sh`` script.

Linux
~~~~~~~~~

To run Phaser Editor on Linux, executes the file ``PhaserEditor2D``.

To run the editor in debug mode, execute the script ``Debug.sh``.

When we build the editor distribution, we set execution permission to a couple of files, however, if for any reason you need to set these permissions again, run the script ``SetExecPermissions.sh`` in a terminal.

Create your first game
----------------------

When the editor opens the first time (or an empty workspace) it shows the **Start** perspective. That perspective provides the links to create a project or an example project:

.. image:: images/start-perspective.png


The **New Project** link opens a wizard where you can set some settings like the initial game size or programming language (JavaScript or TypeScript). 

The **New Example Project** opens a wizard where you can select an example to be cloned as a project.

You can create new projects at any time, by clicking the **New** button of the main toolbar.

.. image:: images/toolbar-new-button.png

Regular Project
~~~~~~~~~~~~~~~

The regular project wizard shows settings related to the game configuration: game size, canvas type, scale mode, etc... In addition, you can select the language to be used in the project: JavaScript 6 or TypeScript.

.. image:: images/new-project-wizard.png


The project structure is pretty similar to a static web project. It contain a ``WebContent`` folder with the files that are part of the game. Especially, it contains the ``index.html`` file. It is the root of the game.

In the following table we provide a short description of each file of the ``WebContent`` folder:

================================== =======================================================
``index.html``                     The entry point of the game. It is very basic, and loads the ``main.js`` file.    
``main.js``                        The Phaser game instance is created. It starts a ``Boot`` scene that is part of this file too.
``jsconfig.json``                  This is the "project" file used by the Language Server include in Phaser Editor. If you are familiar with Visual Studio Code you know what this file is. If you create a TypeScript project, then you will find a ``tsconfig.json`` file instead.
``typings/phaser.d.ts``            The type definitions of the Phaser API. It is used by the Language Server in both type of projects, JavaScript and TypeScript.
``lib/phaser.js``                  Contains the Phaser framework. You can replace it by a newer version or a custom build. It is loaded by the ``index.html`` file, so you have full control to load the Phaser runtime.
``assets/pack.json``               A manifest of the assets used by the game. This file is loaded in the ``Boot`` scene, but you are free to change it. Actually, you are free to change anything. `Learn more about the Asset Pack Editor and pack files <asset-pack-editor.html>`_.
``assets/animations.json``         An empty animations file. It is included in the ``pack.json`` file. `Learn more about sprite animations and the Animations editor <animations-editor.html>`_.
``assets/atlas/textures.atlas``    An empty texture atlas file. `Learn more about the Texture Packer Editor and atlas file <texture-packer-editor.html>`_.
``assets/atlas/textures.json``     The Phaser atlas file derived from the ``texture.atlas`` file ---generated by the `Texture Packer Editor <texture-packer-editor.html>`_---. It is included in the ``pack.json`` file.
``assets/scenes/Scene1.scene``     An empty scene file. You can add objects to this scene using the `Scene Editor <scene-editor.html>`_.
``assets/scenes/Scene1.js``        The compiled scene file. It is included in the ``pack.json`` file and is the thing you see when play the project.
================================== =======================================================


The other top-level folder is the ``Design`` folder, where you can store files related to the design process. Is very common to store there the original images used by the `Texture Packer Editor <texture-packer-editor.html>`_, to generate the Phaser atlas files.

Example Project
~~~~~~~~~~~~~~~

You can create a project based in some of the official Phaser examples or the Phaser Editor examples. It is a valuable resource to learn Phaser.

.. image:: images/new-example-project.png


Play the project
----------------

Phaser Editor has a built-in HTTP server that you can use to play the game project. When you click the **Play** button in the main toolbar, the built-in server is started and the default OS default browser is launched. You can configure the default browser in `Preferences → General → Web Browser`.

.. image:: images/play-project.png


Switching projects
------------------

Phaser Editor allows to to work with many projects at the same time ---it is a feature inherited from the Eclipse IDE-- however, in Phaser game development you don't create references between projects, so we decided to use the **Active project** concept. This means that you are going to say to the IDE what's the project you are working on and the UI will show only the content and commands related to that project.

You can select the **active project** at any time switching to the **Start** perspective or right-clicking on the **Start** button.

TODO missing image of start button menu.

`Learn more about the Start perspective <workbench.html#start-perspective>`_


External editor integration
---------------------------

Phaser Editor uses the Language Server Protocol to edit the HTML, JavaScript, JSON and other web files. It is a protocol used by Visual Studio Code so both editors share a similar experience and configuration. However, is a common practice to use Phaser Editor to design the levels, animations and packs, but use VS Code or any other editor to code the game logic.

In Phaser Editor you can configure an external editor, and some functions will be delagated to that editor. The main toolbar will show a button to launch the external editor and open the current project in it. Other parts of the IDE, like the `Scene Editor <scene-editor.html>`_ and the `Project view <workbench.html#project-view>`_ will launch the external editor too, to edit the code files.

.. image:: images/external-editor-button.png


To configure the external editor go to `Preferences → Phaser Editor → External Code Editor`. You should check the parameter **Open source files in an external editor** to enable the external editor integration. The other paremeters are about to configure the command line arguments. By default, it uses the Visual Studio Code arguments. Important: in the **Program path** parameter you should set the full path to the editor executable.

.. image:: images/external-editor-config.png



Unlock Phaser Editor 2D
-----------------------


