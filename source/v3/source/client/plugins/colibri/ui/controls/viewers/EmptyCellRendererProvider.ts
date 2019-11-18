namespace colibri.ui.controls.viewers {

    export class EmptyCellRendererProvider implements ICellRendererProvider {
        
        getCellRenderer(element: any): ICellRenderer {
            return new EmptyCellRenderer();
        }        
        
        preload(element: any): Promise<PreloadResult> {
            return Controls.resolveNothingLoaded();
        }
    }
}