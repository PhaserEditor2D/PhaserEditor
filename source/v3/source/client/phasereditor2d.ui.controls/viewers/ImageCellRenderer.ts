namespace phasereditor2d.ui.controls.viewers {

    export abstract class ImageCellRenderer implements ICellRenderer {
        abstract getLabel(obj: any): string;

        abstract getImage(obj: any): IImage;

        renderCell(args: RenderCellArgs): void {
            const label = this.getLabel(args.obj);
            const h = this.cellHeight(args);

            const ctx = args.canvasContext;

            const img = this.getImage(args.obj);
            img.paint(ctx, args.x, args.y, h, h);
            ctx.save();

            ctx.fillStyle = Controls.theme.treeItemForeground;

            if (args.view.isSelected(args.obj)) {
                ctx.fillStyle = Controls.theme.treeItemSelectionForeground;
            }

            ctx.fillText(label, args.x + h + 5, args.y + h / 2 + 6);

            ctx.restore();
        }

        cellHeight(args: RenderCellArgs): number {
            return args.view.getCellSize();
        }

        preload(obj: any): Promise<any> {
            return this.getImage(obj).preload();
        }
    }
}