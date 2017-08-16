Build Phaser Editor Guide
=========================

This is a short guide to execute the Phaser Editor sources.
For the moment it requires certain Eclipse skills.

- Download and install Java 8.
- Download Eclipse Oxygen.
- Execute Eclipse.
- Change the access restriction rule to "Ignore". (Preferences > Java > Compiler > Errors/Warnings).
- Create a target platform with the running platform plus the "build/wst_deps" folder.
- Install the E(fx)clipse runtime in the target platform: http://download.eclipse.org/efxclipse/runtime-shared-released/3.0.0/site/
- Clone the repo
- Import all the projects under the "source" folder.
- Open the PhaserEditor.product file of the phasereditor.ide plugin.
- Press on the Run button to execute the editor.

   