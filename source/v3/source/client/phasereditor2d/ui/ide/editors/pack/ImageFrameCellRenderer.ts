namespace phasereditor2d.ui.ide.editors.pack {

    export class ImageFrameCellRenderer implements controls.viewers.ICellRenderer {

        renderCell(args: controls.viewers.RenderCellArgs): void {
            const item = <ImageFrame>args.obj;
            const img = item.getImage();
            const fd = item.getFrameData();

            const renderWidth = args.w;
            const renderHeight = args.h;

            let imgW = fd.src.w;
            let imgH = fd.src.h;

            // compute the right width
            imgW = imgW * (renderHeight / imgH);
            imgH = renderHeight;

            // fix width if it goes beyond the area
            if (imgW > renderWidth) {
                imgH = imgH * (renderWidth / imgW);
                imgW = renderWidth;
            }

            const scale = imgW / fd.src.w;
            var imgX = args.x + (args.center ? renderWidth / 2 - imgW / 2 : 0);
            var imgY = args.y + renderHeight / 2 - imgH / 2;

            const imgDstW = fd.src.w * scale;
            const imgDstH = fd.src.h * scale;

            if (imgDstW > 0 && imgDstH > 0) {

                img.paintFrame(args.canvasContext,
                    fd.src.x, fd.src.y, fd.src.w, fd.src.h,
                    imgX, imgY, imgDstW, imgDstH
                )
            }

        }

        cellHeight(args: controls.viewers.RenderCellArgs): number {
            return args.viewer.getCellSize();
        }

        preload(obj: any): Promise<controls.PreloadResult> {
            const item = <ImageFrame>obj;
            return item.getImage().preload();
        }
    }
}