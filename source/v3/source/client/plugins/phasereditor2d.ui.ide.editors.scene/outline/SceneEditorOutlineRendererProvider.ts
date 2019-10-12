namespace phasereditor2d.ui.ide.editors.scene.outline {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export class SceneEditorOutlineRendererProvider implements controls.viewers.ICellRendererProvider {

        private _editor: SceneEditor;
        private _assetRendererProvider: pack.ui.viewers.AssetPackCellRendererProvider;

        constructor(editor: SceneEditor) {
            this._editor = editor;
            this._assetRendererProvider = new pack.ui.viewers.AssetPackCellRendererProvider();
        }

        getCellRenderer(element: any): controls.viewers.ICellRenderer {

            if (element instanceof Phaser.GameObjects.Image) {

                return new GameObjectCellRenderer();

            } else if (element instanceof Phaser.GameObjects.Container) {

                return new controls.viewers.IconImageCellRenderer(controls.Controls.getIcon(ide.ICON_GROUP));

            } else if (element instanceof Phaser.GameObjects.DisplayList) {

                return new controls.viewers.IconImageCellRenderer(controls.Controls.getIcon(ide.ICON_FOLDER));

            }


            return new controls.viewers.EmptyCellRenderer(false);
        }

        async preload(element: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }


    }
}