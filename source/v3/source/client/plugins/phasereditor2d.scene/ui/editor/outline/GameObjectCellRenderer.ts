namespace phasereditor2d.scene.ui.editor.outline {

    import controls = colibri.ui.controls;

    export class GameObjectCellRenderer implements controls.viewers.ICellRenderer {

        renderCell(args: controls.viewers.RenderCellArgs): void {

            const sprite = <Phaser.GameObjects.GameObject>args.obj;

            if (sprite instanceof Phaser.GameObjects.Image) {

                const { key, frame } = sprite.getEditorTexture();

                const finder = new pack.core.PackFinder();

                // TODO: we should use the same finder for the whole paint.

                /*finder.preload().then(() => {

                    const img = finder.getAssetPackItemImage(key, frame);

                    if (img) {
                        img.paint(args.canvasContext, args.x, args.y, args.w, args.h, false);
                    }
                });*/
            }
        }

        cellHeight(args: colibri.ui.controls.viewers.RenderCellArgs): number {

            if (args.obj instanceof Phaser.GameObjects.Image) {
                return args.viewer.getCellSize();
            }

            return colibri.ui.controls.ROW_HEIGHT;
        }

        preload(args: controls.viewers.PreloadCellArgs): Promise<colibri.ui.controls.PreloadResult> {

            /*
            const finder = new pack.core.PackFinder();

            return finder.preload();
            */

            return controls.Controls.resolveNothingLoaded();
        }
    }

}