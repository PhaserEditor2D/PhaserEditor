namespace colibri.ui.ide {

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
            const file = this.getInput();
            
            if (!file) {
                return Workbench.getWorkbench().getWorkbenchIcon(ICON_FILE);
            }

            const wb = Workbench.getWorkbench();
            const ct = wb.getContentTypeRegistry().getCachedContentType(file);
            const icon = wb.getContentTypeIcon(ct);

            return icon;
        }
    }
}