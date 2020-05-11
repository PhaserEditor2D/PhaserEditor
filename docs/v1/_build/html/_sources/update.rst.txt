.. include:: _header.rst

Update and Upgrade
==================

In Phaser Editor we do a distinction between update and upgrade. 

In theory, update and upgrade are the same, the difference is that by default the editor is going to be updated only with the changes made for the current major version.

For example, if you download version ``1.4.0`` the editor will receive automatic updates for version ``1.4.1``, ``1.4.2`` and so on.

If you want to upgrade the editor to version ``1.5.0`` then you have to do it manually.

Update
------

As we said, the update is done automatically. Each time the editor starts it looks for updates, if there are updates available then it will ask confirmation to install them.

However, you can check for updates at any time manually, just click on ``Help > Check for Updates``.

The installing of the updates is straightforward, it will show the new plugins to be installed/updates and the license of them. Just press **OK** for everything.

If you find any issue please `contact us <https://github.com/PhaserEditor2D/PhaserEditor/issues>` _.

Upgrade
-------

To upgrade from one version to other these are the steps:

- Open the Available Software Sites ``Windows > Preferences > Install/Update > Available Software Sites``.
- Press the Add button to add a new update site, the one with the version you are interested in:

.. image:: images/AddUpdateSite1.png
	:alt: Add update site.
	
- Press **OK**, ensure the new site is checked and press OK to close the preferences dialog.

.. image:: images/AddUpdateSite2.png
	:alt: Confirm update sites.

- Manually check for updates (``Help > Check for Updates``), it should prompt the new updates, install them as usual.
