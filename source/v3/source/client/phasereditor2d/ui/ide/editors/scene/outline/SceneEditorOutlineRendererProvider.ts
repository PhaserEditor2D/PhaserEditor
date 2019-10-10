namespace phasereditor2d.ui.ide.editors.scene.outline {

    export class SceneEditorOutlineRendererProvider implements controls.viewers.ICellRendererProvider {

        private _editor: SceneEditor;
        private _assetRendererProvider: pack.viewers.AssetPackCellRendererProvider;
        //TODO: we should use the asset finder of the editor!
        private _packs: pack.AssetPack[];

        constructor(editor: SceneEditor) {
            this._editor = editor;
            this._assetRendererProvider = new pack.viewers.AssetPackCellRendererProvider();
            this._packs = null;
        }

        getCellRenderer(element: any): controls.viewers.ICellRenderer {

            if (this._packs !== null) {

                if (element instanceof Phaser.GameObjects.Image) {

                    return new GameObjectCellRenderer(this._packs);

                } else if (element instanceof Phaser.GameObjects.Container) {

                    return new controls.viewers.IconImageCellRenderer(controls.Controls.getIcon(ide.ICON_GROUP));

                } else if (element instanceof Phaser.GameObjects.DisplayList) {

                    return new controls.viewers.IconImageCellRenderer(controls.Controls.getIcon(ide.ICON_FOLDER));

                }

            }

            return new controls.viewers.EmptyCellRenderer(false);
        }

        async preload(element: any): Promise<controls.PreloadResult> {

            if (this._packs === null) {
                return pack.AssetPackUtils.getAllPacks().then(packs => {
                    this._packs = packs;
                    return controls.PreloadResult.RESOURCES_LOADED;
                });
            }

            return controls.Controls.resolveNothingLoaded();
        }


    }
}