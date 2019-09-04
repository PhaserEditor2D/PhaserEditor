/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.files {

    import io = phasereditor2d.core.io;
    import viewers = phasereditor2d.ui.controls.viewers;

    export class FileTreeContentProvider implements viewers.ITreeContentProvider {

        getRoots(input: any): any[] {

            if (input instanceof io.FilePath) {
                return [input];
            }

            if (input instanceof Array) {
                return input;
            }
            
            return this.getChildren(input);
        }

        getChildren(parent: any): any[] {
            return (<io.FilePath>parent).getFiles();
        }

    }
}