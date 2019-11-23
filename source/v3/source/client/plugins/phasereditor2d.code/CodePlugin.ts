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
                    [ui.editors.JavaScriptEditor.getFactory()])
            );

        }
    }

    colibri.ui.ide.Workbench.getWorkbench().addPlugin(CodePlugin.getInstance());
}