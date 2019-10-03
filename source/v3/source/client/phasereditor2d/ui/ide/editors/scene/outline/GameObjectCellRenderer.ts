namespace phasereditor2d.ui.ide.editors.scene.outline {

    export class GameObjectCellRenderer implements controls.viewers.ICellRenderer {

        renderCell(args: controls.viewers.RenderCellArgs): void {
            const { renderer, asset } = this.getRenderer(args);

            if (renderer) {
                const args2 = args.clone();
                args2.obj = asset;
                renderer.renderCell(args2);
            }

        }

        private getRenderer(args: controls.viewers.RenderCellArgs) {
            const sprite = <Phaser.GameObjects.GameObject>args.obj;
            const asset = sprite.getEditorAsset();

            if (asset) {
                const provider = new pack.viewers.AssetPackCellRendererProvider();
                return {
                    renderer: provider.getCellRenderer(asset),
                    asset: asset
                };
            }

            return {
                renderer: null,
                asset: null
            };
        }

        cellHeight(args: controls.viewers.RenderCellArgs): number {

            const { renderer, asset } = this.getRenderer(args);

            if (renderer) {
                return args.viewer.getCellSize();
            }

            return controls.ROW_HEIGHT;
        }

        preload(obj: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }


    }

}