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
                    case "spritesheet":
                        return new controls.viewers.FolderCellRenderer();
                    default: 
                        break;
                }
            } else if (element instanceof controls.ImageFrame) {
                return new controls.viewers.ImageCellRenderer();
            }

            return new controls.viewers.EmptyCellRenderer();
        }

        preload(element: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }
    }

}