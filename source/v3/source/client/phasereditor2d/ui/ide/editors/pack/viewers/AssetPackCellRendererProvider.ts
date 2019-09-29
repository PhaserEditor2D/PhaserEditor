namespace phasereditor2d.ui.ide.editors.pack.viewers {

    export class AssetPackCellRendererProvider implements controls.viewers.ICellRendererProvider {

        getCellRenderer(element: any): controls.viewers.ICellRenderer {

            if (element instanceof AssetPackItem) {
                const type = element.getType();
                switch (type) {
                    case "image":
                        return new ImageAssetPackItemCellRenderer();
                    case "multiatlas":
                    case "atlas":
                    case "unityAtlas":
                    case "atlasXML":
                        return new controls.viewers.FolderCellRenderer();
                    case "spritesheet":
                        return new SpriteSheetPackItemCellRenderer();
                    default:
                        break;
                }
            } else if (element instanceof ImageFrame) {
                return new ImageFrameCellRenderer();
            }

            return new controls.viewers.EmptyCellRenderer();
        }

        preload(element: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }
    }

}