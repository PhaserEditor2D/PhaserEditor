namespace phasereditor2d.pack.ui.dialogs {

    import io = colibri.core.io;

    export class NewAssetPackFileWizardExtension extends files.ui.dialogs.NewFileContentExtension {

        constructor() {
            super({
                id: "phasereditor2d.pack.ui.wizards.NewAssetPackFileWizardExtension",
                wizardName: "Asset Pack File",
                icon: AssetPackPlugin.getInstance().getIcon(ICON_ASSET_PACK),
                initialFileName: "asset-pack",
                fileExtension: "json",
                fileContent: `{
                    "section1": {
                        "files": [
                        ]
                    },
                    "meta": {
                        "app": "Phaser Editor 2D - Asset Pack Editor",
                        "contentType": "Phaser v3 Asset Pack",
                        "url": "https://phasereditor2d.com",
                        "version": "2"
                    }
                }`
            });
        }

      
        getInitialFileLocation() {
            return super.findInitialFileLocationBasedOnContentType(pack.core.contentTypes.CONTENT_TYPE_ASSET_PACK);
        }
    }
}

