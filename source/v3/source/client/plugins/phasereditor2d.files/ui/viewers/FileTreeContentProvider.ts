namespace phasereditor2d.files.ui.views {

    import controls = colibri.ui.controls;
    import viewers = controls.viewers;
    import core = colibri.core;

    export class FileTreeContentProvider implements viewers.ITreeContentProvider {

        getRoots(input: any): any[] {

            if (input instanceof core.io.FilePath) {
                return [input];
            }

            if (input instanceof Array) {
                return input;
            }

            return this.getChildren(input);
        }

        getChildren(parent: any): any[] {
            return (<core.io.FilePath>parent).getFiles();
        }

    }
}