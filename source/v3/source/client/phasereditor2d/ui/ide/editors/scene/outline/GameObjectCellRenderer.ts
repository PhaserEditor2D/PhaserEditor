namespace phasereditor2d.ui.ide.editors.scene.outline {

    export class GameObjectCellRenderer implements controls.viewers.ICellRenderer {

        private _packs: pack.AssetPack[];

        constructor(packs: pack.AssetPack[]) {
            this._packs = packs;
        }

        renderCell(args: controls.viewers.RenderCellArgs): void {

            const sprite = <Phaser.GameObjects.GameObject>args.obj;

            if (sprite instanceof Phaser.GameObjects.Image) {

                const { key, frame } = sprite.getEditorTexture();

                const finder = new pack.AssetFinder(this._packs);
                const img = finder.getAssetPackItemImage(key, frame);

                if (img) {
                    img.paint(args.canvasContext, args.x, args.y, args.w, args.h, false);
                }
            }

        }

        cellHeight(args: controls.viewers.RenderCellArgs): number {

            if (args.obj instanceof Phaser.GameObjects.Image) {
                return args.viewer.getCellSize();
            }

            return controls.ROW_HEIGHT;
        }

        preload(obj: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }


    }

}