/// <reference path="./TreeViewer.ts" />

namespace phasereditor2d.ui.controls.viewers {
    
    export class GridViewer extends TreeViewer {

        constructor(...classList : string[]) {
            super("GridViewer", ...classList);
        }    

    }
}