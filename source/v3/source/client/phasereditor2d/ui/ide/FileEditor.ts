namespace phasereditor2d.ui.ide {

    import io = core.io;

    export abstract class FileEditor extends EditorPart {
        constructor(id : string) {
            super(id)
        }


        public setInput(file : io.FilePath) {
            super.setInput(file);
            this.setTitle(file.getName());
        }

        public getInput() : core.io.FilePath {
            return super.getInput();
        }
    }
}