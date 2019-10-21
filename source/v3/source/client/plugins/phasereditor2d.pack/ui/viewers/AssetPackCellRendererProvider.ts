namespace phasereditor2d.pack.ui.viewers {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export class AssetPackCellRendererProvider implements controls.viewers.ICellRendererProvider {

        private _layout: "grid" | "tree";

        constructor(layout: "grid" | "tree") {
            this._layout = layout;
        }

        getCellRenderer(element: any): controls.viewers.ICellRenderer {

            if (typeof (element) === "string") {

                return new controls.viewers.IconImageCellRenderer(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FOLDER));

            } else if (element instanceof core.AssetPackItem) {

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

            if (this._layout === "grid") {
                return new controls.viewers.IconGridCellRenderer(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FILE));
            }

            return new controls.viewers.IconImageCellRenderer(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FILE));
        }

        preload(element: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }
    }

}