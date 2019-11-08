namespace phasereditor2d.pack.ui.wizards {

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
    }
}