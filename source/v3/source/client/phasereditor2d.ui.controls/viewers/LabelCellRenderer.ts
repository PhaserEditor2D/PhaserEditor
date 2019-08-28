namespace phasereditor2d.ui.controls.viewers {

    export abstract class LabelCellRenderer implements ICellRenderer {

        renderCell(args: RenderCellArgs): void {
            const label = this.getLabel(args.obj);
            const img = this.getImage(args.obj);
            let x = args.x;

            const ctx = args.canvasContext;
            ctx.fillStyle = Controls.theme.treeItemForeground;

            if (img) {
                const h = this.cellHeight(args);
                img.paint(ctx, x, args.y, 16, h);
                x += 20;
            }

            ctx.save();
            if (args.view.isSelected(args.obj)) {
                ctx.fillStyle = Controls.theme.treeItemSelectionForeground;
            }
            ctx.fillText(label, x, args.y + 15);
            ctx.restore();
        }

        abstract getLabel(obj: any): string;

        abstract getImage(obj: any): controls.IIcon;

        cellHeight(args: RenderCellArgs): number {
            return 20;
        }

        preload(): Promise<any> {
            return Promise.resolve();
        }
    }

}