/// <reference path="./Viewer.ts"/>

namespace phasereditor2d.ui.controls.viewers {
    export interface ITreeContentProvider {
        getRoots(input: any): any[];

        getChildren(parent: any): any[];
    }
}