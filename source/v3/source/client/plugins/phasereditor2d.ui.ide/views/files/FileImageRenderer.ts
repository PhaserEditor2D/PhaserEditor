namespace phasereditor2d.ui.ide.views.files {

    import io = phasereditor2d.core.io;
    import viewers = phasereditor2d.ui.controls.viewers;

    export class FileImageRenderer extends viewers.ImageCellRenderer {

        getLabel(file: io.FilePath): string {
            return file.getName();
        }

        getImage(file: io.FilePath): controls.IImage {
            return Workbench.getWorkbench().getFileImage(file);
        }
    }
}