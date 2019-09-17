namespace phasereditor2d.ui.ide.editors.pack {
    
    import io = core.io

    export class AssetPackEditorFactory extends EditorFactory {

        constructor() {
            super("phasereditor2d.AssetPackEditorFactory");
        }

        public acceptInput(input: any): boolean {
            return input instanceof io.FilePath && input.getExtension() === "json";
        }        
        
        public createEditor(): EditorPart {
            return new AssetPackEditor();
        }   

    }

    export class AssetPackEditor extends FileEditor {

        public constructor() {
            super("phasereditor2d.AssetPackEditor");
            this.addClass("AssetPackEditor");
        }

        public static getFactory() : AssetPackEditorFactory {
            return new AssetPackEditorFactory();
        }

        public createPart() : void {
            super.createPart();
            this.updateContent();
        }

        private async updateContent() {
            const file = this.getInput();
            if (!file) {
                return;
            }

            const content = await Workbench.getWorkbench().getFileStorage().getFileString(file);
            this.getElement().innerHTML = content;
        }

        public setInput(file : io.FilePath) : void {
            super.setInput(file);
            this.updateContent();
        }
    }
}