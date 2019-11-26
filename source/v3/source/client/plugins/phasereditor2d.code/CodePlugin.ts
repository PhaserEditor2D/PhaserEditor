namespace phasereditor2d.code {

    import controls = colibri.ui.controls;

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
                        new ui.editors.MonacoEditorFactory("javascript", webContentTypes.core.CONTENT_TYPE_JAVASCRIPT),
                        new ui.editors.MonacoEditorFactory("typescript", webContentTypes.core.CONTENT_TYPE_SCRIPT),
                        new ui.editors.MonacoEditorFactory("html", webContentTypes.core.CONTENT_TYPE_HTML),
                        new ui.editors.MonacoEditorFactory("css", webContentTypes.core.CONTENT_TYPE_CSS),
                        new ui.editors.MonacoEditorFactory("json", webContentTypes.core.CONTENT_TYPE_JSON),
                        new ui.editors.MonacoEditorFactory("xml", webContentTypes.core.CONTENT_TYPE_XML),
                        new ui.editors.MonacoEditorFactory("text", webContentTypes.core.CONTENT_TYPE_TEXT),
                    ])
            );
        }

        async starting() {

            monaco.languages.typescript.typescriptDefaults.setDiagnosticsOptions({
                noSemanticValidation: true
            });

            window.addEventListener(controls.EVENT_THEME_CHANGED, e => {

                let monacoTheme = "vs";

                if (controls.Controls.getTheme().dark) {
                    monacoTheme = "vs-dark";
                }

                monaco.editor.setTheme(monacoTheme);

            });
        }
    }

    colibri.ui.ide.Workbench.getWorkbench().addPlugin(CodePlugin.getInstance());
}