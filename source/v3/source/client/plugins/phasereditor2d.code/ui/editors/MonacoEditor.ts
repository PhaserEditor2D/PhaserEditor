namespace phasereditor2d.code.ui.editors {

    import io = colibri.core.io;

    export class MonacoEditorFactory extends colibri.ui.ide.EditorFactory {

        private _language: string;
        private _contentType: string;

        constructor(language: string, contentType: string) {
            super("phasereditor2d.core.ui.editors.MonacoEditorFactory#" + language);

            this._language = language;
            this._contentType = contentType;
        }

        acceptInput(input: any): boolean {

            if (input instanceof io.FilePath) {

                const contentType = colibri.ui.ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);

                return this._contentType === contentType;
            }

            return false;
        }

        createEditor(): colibri.ui.ide.EditorPart {
            return new MonacoEditor(this._language);
        }

    }

    export class MonacoEditor extends colibri.ui.ide.FileEditor {

        private static _factory: colibri.ui.ide.EditorFactory;

        private _monacoEditor: monaco.editor.IStandaloneCodeEditor;
        private _language: string;

        constructor(language: string) {

            super("phasereditor2d.core.ui.editors.JavaScriptEditor");

            this._language = language;
        }

        protected createPart(): void {

            this._monacoEditor = monaco.editor.create(this.getElement(), {
                language: this._language
            });

            this.updateContent();
        }

        setInput(file: io.FilePath) {

            super.setInput(file);

            this.updateContent();
        }

        private async updateContent() {

            const file = this.getInput();

            if (!file) {
                return;
            }

            const content = await colibri.ui.ide.FileUtils.preloadAndGetFileString(file);

            this._monacoEditor.setValue(content);
        }

        layout() {

            super.layout();

            this._monacoEditor.layout();
        }

        protected onEditorInputContentChanged() {

        }
    }
}