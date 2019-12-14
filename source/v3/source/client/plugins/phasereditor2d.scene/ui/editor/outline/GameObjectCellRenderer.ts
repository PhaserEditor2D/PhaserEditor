namespace phasereditor2d.scene.ui.editor.outline {

    import controls = colibri.ui.controls;

    export class GameObjectCellRenderer implements controls.viewers.ICellRenderer {

        renderCell(args: controls.viewers.RenderCellArgs): void {

            const sprite = <Phaser.GameObjects.GameObject>args.obj;

            if (sprite instanceof Phaser.GameObjects.Image) {

                const { key, frame } = sprite.getEditorTexture();

                // TODO: we should paint the object using the Phaser rendering system, 
                // maybe using a an offline texture that is updated each time the object is updated.

                const finder = new pack.core.PackFinder();

                finder.preload().then(() => {

                    const img = finder.getAssetPackItemImage(key, frame);

                    if (img) {
                        img.paint(args.canvasContext, args.x, args.y, args.w, args.h, false);
                    }
                });
            }
        }

        cellHeight(args: colibri.ui.controls.viewers.RenderCellArgs): number {

            if (args.obj instanceof Phaser.GameObjects.Image) {
                return args.viewer.getCellSize();
            }

            return colibri.ui.controls.ROW_HEIGHT;
        }

        async preload(args: controls.viewers.PreloadCellArgs): Promise<colibri.ui.controls.PreloadResult> {
            
            const finder = new pack.core.PackFinder();

            return finder.preload();
        }
    }
}