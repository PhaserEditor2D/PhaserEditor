namespace phasereditor2d.code {

    export class CodePlugin extends colibri.ui.ide.Plugin {

        private static _instance: CodePlugin;

        static getInstance() {

            if (!this._instance) {
                this._instance = new CodePlugin();
            }

            return this._instance;
        }

        constructor() {
            super("phasereditor2d.core")
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            reg.addExtension(colibri.ui.ide.EditorExtension.POINT_ID,
                new colibri.ui.ide.EditorExtension("phasereditor2d.core.ui.editors",
                    [
                        new ui.editors.MonacoEditorFactory("javascript", "js"),
                        new ui.editors.MonacoEditorFactory("typescript", "ts"),
                        new ui.editors.MonacoEditorFactory("html", "html"),
                        new ui.editors.MonacoEditorFactory("css", "css"),
                        new ui.editors.MonacoEditorFactory("text", "txt"),
                    ])
            );

        }
    }

    colibri.ui.ide.Workbench.getWorkbench().addPlugin(CodePlugin.getInstance());
}