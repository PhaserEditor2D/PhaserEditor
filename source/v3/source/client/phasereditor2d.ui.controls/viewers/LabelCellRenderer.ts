namespace phasereditor2d.ui.controls.viewers {

    export abstract class LabelCellRenderer implements ICellRenderer {

        renderCell(args: RenderCellArgs): void {
            const img = this.getImage(args.obj);
            let x = args.x;

            const ctx = args.canvasContext;
            if (img) {
                img.paint(ctx, x, args.y, 16, args.h);
            }
        }
        
        abstract getImage(obj: any): controls.IIcon;

        cellHeight(args: RenderCellArgs): number {
            return controls.ROW_HEIGHT;
        }

        preload(obj: any): Promise<any> {
            return Promise.resolve();
        }
    }

}