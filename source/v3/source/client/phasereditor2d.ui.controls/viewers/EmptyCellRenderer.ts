namespace phasereditor2d.ui.controls.viewers {
    export class EmptyCellRenderer implements ICellRenderer {
        
        renderCell(args: RenderCellArgs): void {
            
        }        
        
        cellHeight(args: RenderCellArgs): number {
            return args.viewer.getCellSize();
        }

        preload(obj: any): Promise<PreloadResult> {
            return Controls.resolveNothingLoaded();
        }
    }
}