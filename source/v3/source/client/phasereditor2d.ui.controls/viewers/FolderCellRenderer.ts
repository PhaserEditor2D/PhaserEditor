namespace phasereditor2d.ui.controls.viewers {
    export class FolderCellRenderer implements ICellRenderer {

        renderCell(args: RenderCellArgs): void {
            const ctx = args.canvasContext;
            ctx.save();
            ctx.globalAlpha = 0.5;
            ctx.fillStyle = Controls.theme.treeItemForeground;
            const header = Math.floor(args.h * 0.15);
            let w = args.h;
            ctx.fillRect(args.x, args.y + 2, (w - 2) * 0.6, header);
            ctx.fillRect(args.x, args.y + 2 + header, w - 2, args.h - 2 - header - 2);
            ctx.restore();
        }        
        
        cellHeight(args: RenderCellArgs): number {
            return args.viewer.getCellSize();
        }
        
        preload(obj: any): Promise<PreloadResult> {
            return Controls.resolveNothingLoaded();
        }

    }
}