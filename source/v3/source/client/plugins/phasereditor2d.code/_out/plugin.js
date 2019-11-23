var phasereditor2d;
(function (phasereditor2d) {
    var code;
    (function (code) {
        class CodePlugin extends colibri.ui.ide.Plugin {
            constructor() {
                super("phasereditor2d.core");
            }
            static getInstance() {
                if (!this._instance) {
                    this._instance = new CodePlugin();
                }
                return this._instance;
            }
            registerExtensions(reg) {
                reg.addExtension(colibri.ui.ide.EditorExtension.POINT_ID, new colibri.ui.ide.EditorExtension("phasereditor2d.core.ui.editors", [code.ui.editors.JavaScriptEditor.getFactory()]));
            }
        }
        code.CodePlugin = CodePlugin;
        colibri.ui.ide.Workbench.getWorkbench().addPlugin(CodePlugin.getInstance());
    })(code = phasereditor2d.code || (phasereditor2d.code = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var code;
    (function (code) {
        var ui;
        (function (ui) {
            var editors;
            (function (editors) {
                var io = colibri.core.io;
                class JavaScriptEditorFactory extends colibri.ui.ide.EditorFactory {
                    constructor() {
                        super("phasereditor2d.core.ui.editors.JavaScriptEditorFactory");
                    }
                    acceptInput(input) {
                        if (input instanceof io.FilePath) {
                            return input.getExtension() === "js";
                        }
                        return false;
                    }
                    createEditor() {
                        return new JavaScriptEditor();
                    }
                }
                editors.JavaScriptEditorFactory = JavaScriptEditorFactory;
                class JavaScriptEditor extends colibri.ui.ide.EditorPart {
                    constructor() {
                        super("phasereditor2d.core.ui.editors.JavaScriptEditor");
                    }
                    static getFactory() {
                        if (!this._factory) {
                            this._factory = new JavaScriptEditorFactory();
                        }
                        return this._factory;
                    }
                    createPart() {
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
                editors.JavaScriptEditor = JavaScriptEditor;
            })(editors = ui.editors || (ui.editors = {}));
        })(ui = code.ui || (code.ui = {}));
    })(code = phasereditor2d.code || (phasereditor2d.code = {}));
})(phasereditor2d || (phasereditor2d = {}));
