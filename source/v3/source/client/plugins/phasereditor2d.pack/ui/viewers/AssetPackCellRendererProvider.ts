namespace phasereditor2d.pack.ui.viewers {

    import controls = colibri.ui.controls;

    export class AssetPackCellRendererProvider implements controls.viewers.ICellRendererProvider {

        getCellRenderer(element: any): controls.viewers.ICellRenderer {

            if (element instanceof core.AssetPackItem) {

                const type = element.getType();

                switch (type) {
                    case core.IMAGE_TYPE:
                        return new ImageAssetPackItemCellRenderer();
                    case core.MULTI_ATLAS_TYPE:
                    case core.ATLAS_TYPE:
                    case core.UNITY_ATLAS_TYPE:
                    case core.ATLAS_XML_TYPE:
                    case core.SPRITESHEET_TYPE:
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