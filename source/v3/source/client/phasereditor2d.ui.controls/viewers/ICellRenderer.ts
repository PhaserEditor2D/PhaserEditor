namespace phasereditor2d.ui.controls.viewers {

    export interface ICellRenderer {
        renderCell(args: RenderCellArgs): void;

        cellHeight(args: RenderCellArgs): number;

        preload(obj: any): Promise<any>;
    }
    
}