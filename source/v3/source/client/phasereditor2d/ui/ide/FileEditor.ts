namespace phasereditor2d.ui.ide {

    import io = core.io;

    export abstract class FileEditor extends EditorPart {
        constructor(id: string) {
            super(id)
        }


        setInput(file: io.FilePath) {
            super.setInput(file);
            this.setTitle(file.getName());
        }

        getInput(): core.io.FilePath {
            return super.getInput();
        }

        getIcon() {
            const wb = Workbench.getWorkbench();
            const ct = wb.getContentTypeRegistry().getCachedContentType(this.getInput());
            const icon = wb.getContentTypeIcon(ct);
            return icon;
        }
    }
}