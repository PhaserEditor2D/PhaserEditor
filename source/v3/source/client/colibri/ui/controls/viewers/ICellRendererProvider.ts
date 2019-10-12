/// <reference path="../../controls/Controls.ts" />

namespace colibri.ui.controls.viewers {

    export interface ICellRendererProvider {
        
        getCellRenderer(element: any): ICellRenderer;

        preload(element: any) : Promise<PreloadResult>;
    }

}