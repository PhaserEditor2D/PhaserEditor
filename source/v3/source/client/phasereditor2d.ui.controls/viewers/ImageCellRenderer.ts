namespace phasereditor2d.ui.controls.viewers {

    export abstract class ImageCellRenderer implements ICellRenderer {
        private _center: boolean;

        constructor(center: boolean) {
            this._center = center;
        }

        abstract getImage(obj: any): IImage;

        renderCell(args: RenderCellArgs): void {
            const img = this.getImage(args.obj);
            img.paint(args.canvasContext, args.x, args.y, args.w, args.h, this._center);
        }

        cellHeight(args: RenderCellArgs): number {
            return args.view.getCellSize();
        }

        preload(obj: any): Promise<any> {
            return this.getImage(obj).preload();
        }
    }
}