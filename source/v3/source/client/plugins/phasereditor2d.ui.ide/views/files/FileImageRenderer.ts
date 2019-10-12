namespace phasereditor2d.ui.ide.views.files {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;

    export class FileImageRenderer extends controls.viewers.ImageCellRenderer {

        getLabel(file: core.io.FilePath): string {
            return file.getName();
        }

        getImage(file: core.io.FilePath): controls.IImage {
            return ide.Workbench.getWorkbench().getFileImage(file);
        }
    }
}