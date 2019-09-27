namespace phasereditor2d.ui.ide.editors.pack {

    export class AssetPackCellRendererProvider implements controls.viewers.ICellRendererProvider {

        getCellRenderer(element: any): controls.viewers.ICellRenderer {

            if (element instanceof AssetPackItem) {
                const type = element.getType();
                switch (type) {
                    case "image":
                        return new ImageAssetPackItemCellRenderer();
                    default:
                        break;
                }
            } else if (element instanceof ImageFrame) {
                return new ImageFrameCellRenderer(true);
            }

            return new controls.viewers.EmptyCellRenderer();
        }

        preload(element: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }
    }

}