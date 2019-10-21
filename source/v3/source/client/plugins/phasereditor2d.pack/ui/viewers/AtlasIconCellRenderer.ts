namespace phasereditor2d.pack.ui.viewers {

    import controls = colibri.ui.controls;

    export class ImageFrameContainerIconCellRenderer implements controls.viewers.ICellRenderer {

        renderCell(args: controls.viewers.RenderCellArgs): void {
            const packItem = <core.AssetPackItem>args.obj;

            if (core.AssetPackUtils.isImageFrameContainer(packItem)) {

                const frames = core.AssetPackUtils.getImageFrames(packItem);

                if (frames.length > 0) {

                    const img = frames[0].getImage();
                    img.paint(args.canvasContext, args.x, args.y, args.w, args.h, args.center);
                }
            }
        }

        cellHeight(args: controls.viewers.RenderCellArgs): number {
            return args.viewer.getCellSize();
        }

        preload(obj: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }
    }
}