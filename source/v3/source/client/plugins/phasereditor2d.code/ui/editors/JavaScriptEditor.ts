namespace phasereditor2d.code.ui.editors {

    import io = colibri.core.io;

    export class JavaScriptEditorFactory extends colibri.ui.ide.EditorFactory {

        constructor() {
            super("phasereditor2d.core.ui.editors.JavaScriptEditorFactory");
        }

        acceptInput(input: any): boolean {

            if (input instanceof io.FilePath) {

                return input.getExtension() === "js";
            }

            return false;
        }

        createEditor(): colibri.ui.ide.EditorPart {
            return new JavaScriptEditor();
        }

    }

    export class JavaScriptEditor extends colibri.ui.ide.EditorPart {

        private static _factory: colibri.ui.ide.EditorFactory;

        static getFactory() {

            if (!this._factory) {
                this._factory = new JavaScriptEditorFactory();
            }


            return this._factory;
        }

        constructor() {
            super("phasereditor2d.core.ui.editors.JavaScriptEditor");
        }

        protected createPart(): void {

            const container = document.createElement("div");

            const editor = monaco.editor.create(this.getElement(), {
                value: [
                    'function x() {',
                    '\tconsole.log("Hello world!");',
                    '}'
                ].join('\n'),
                language: 'javascript'
            });
        }
    }
}