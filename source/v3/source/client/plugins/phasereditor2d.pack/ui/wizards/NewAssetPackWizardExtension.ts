namespace phasereditor2d.pack.ui.wizards {

    import io = colibri.core.io;

    export class NewAssetPackFileWizardExtension extends ide.ui.wizards.NewWizardExtension {

        constructor() {
            super({
                id: "phasereditor2d.pack.ui.wizards.NewAssetPackFileWizardExtension",
                wizardName: "Asset Pack File",
                icon: AssetPackPlugin.getInstance().getIcon(ICON_ASSET_PACK),
                fileExtension: "json",
                initialFileName: "asset-pack"
            });
        }

        getInitialFileLocation() {
            return super.findInitialFileLocationBasedOnContentType(pack.core.contentTypes.CONTENT_TYPE_ASSET_PACK);
        }
    }
}