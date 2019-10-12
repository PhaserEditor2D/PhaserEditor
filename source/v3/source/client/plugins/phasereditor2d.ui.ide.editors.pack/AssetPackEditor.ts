namespace phasereditor2d.ui.ide.editors.pack {
    
    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;
    import io = core.io

    export class AssetPackEditorFactory extends ide.EditorFactory {

        constructor() {
            super("phasereditor2d.AssetPackEditorFactory");
        }

        acceptInput(input: any): boolean {
            if (input instanceof io.FilePath) {
                const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                return contentType === CONTENT_TYPE_ASSET_PACK;
            }
            return false;
        }        
        
        createEditor(): ide.EditorPart {
            return new AssetPackEditor();
        }   

    }

    export class AssetPackEditor extends ide.FileEditor {

        constructor() {
            super("phasereditor2d.AssetPackEditor");
            this.addClass("AssetPackEditor");
        }

        static getFactory() : AssetPackEditorFactory {
            return new AssetPackEditorFactory();
        }

        protected createPart() : void {
            this.updateContent();
        }

        private async updateContent() {
            const file = this.getInput();
            if (!file) {
                return;
            }

            const content = await ide.FileUtils.preloadAndGetFileString(file);
            
            this.getElement().innerHTML = content;
        }

        setInput(file : io.FilePath) : void {
            super.setInput(file);
            this.updateContent();
        }
    }
}