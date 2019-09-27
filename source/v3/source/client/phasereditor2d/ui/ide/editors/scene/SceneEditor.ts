/// <reference path="../../EditorBlocksProvider.ts" />

namespace phasereditor2d.ui.ide.editors.scene {

    import io = core.io;

    class SceneEditorFactory extends EditorFactory {
        
        constructor() {
            super("phasereditor2d.SceneEditorFactory");
        }

        acceptInput(input: any): boolean {
            if (input instanceof io.FilePath) {
                const contentType = Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                return contentType === CONTENT_TYPE_SCENE;
            }
            return false;
        }
        
        createEditor(): EditorPart {
            return new SceneEditor(); 
        }


    }

   

    export class SceneEditor extends FileEditor {
        
        private _blocksProvider : SceneEditorBlocksProvider;

        static getFactory(): EditorFactory {
            return new SceneEditorFactory();
        }

        constructor() {
            super("phasereditor2d.SceneEditor");
            
            this._blocksProvider = new SceneEditorBlocksProvider();
        }

        protected createPart() {
            const label = document.createElement("label");
            label.innerHTML = "Hello Scene Editor";
            this.getElement().appendChild(label);
        }

        getBlocksProvider() {
            return this._blocksProvider;
        }
    }

}