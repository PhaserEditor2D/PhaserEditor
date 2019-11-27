/// <reference path="./MonacoEditor.ts" />

namespace phasereditor2d.code.ui.editors {

    export class JavaScriptEditorFactory extends MonacoEditorFactory {
        
        constructor() {
            super("javascript", webContentTypes.core.CONTENT_TYPE_JAVASCRIPT);
        }

        createEditor(): colibri.ui.ide.EditorPart {
            return new JavaScriptEditor();
        }
    }

    export class JavaScriptEditor extends MonacoEditor {

        constructor() {
            super("javascript");
        }
    }
}