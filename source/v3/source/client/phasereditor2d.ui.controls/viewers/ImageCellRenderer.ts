namespace phasereditor2d.ui.controls.viewers {

    export abstract class ImageCellRenderer implements ICellRenderer {

        abstract getImage(obj: any): IImage;

        renderCell(args: RenderCellArgs): void {
            const img = this.getImage(args.obj);
            img.paint(args.canvasContext, args.x, args.y, args.w, args.h, args.center);
        }

        cellHeight(args: RenderCellArgs): number {
            return args.viewer.getCellSize();
        }

        preload(obj: any): Promise<any> {
            return this.getImage(obj).preload();
        }
    }
}