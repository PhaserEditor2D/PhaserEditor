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
                reg.addExtension(colibri.ui.ide.EditorExtension.POINT_ID, new colibri.ui.ide.EditorExtension("phasereditor2d.core.ui.editors", [
                    new code.ui.editors.MonacoEditorFactory("javascript", "js"),
                    new code.ui.editors.MonacoEditorFactory("typescript", "ts"),
                    new code.ui.editors.MonacoEditorFactory("html", "html"),
                    new code.ui.editors.MonacoEditorFactory("css", "css"),
                    new code.ui.editors.MonacoEditorFactory("text", "txt"),
                ]));
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
                class MonacoEditorFactory extends colibri.ui.ide.EditorFactory {
                    constructor(language, fileExtension) {
                        super("phasereditor2d.core.ui.editors.MonacoEditorFactory#" + language);
                        this._language = language;
                        this._fileExtension = fileExtension;
                    }
                    acceptInput(input) {
                        if (input instanceof io.FilePath) {
                            return input.getExtension() === this._fileExtension;
                        }
                        return false;
                    }
                    createEditor() {
                        return new MonacoEditor(this._language);
                    }
                }
                editors.MonacoEditorFactory = MonacoEditorFactory;
                class MonacoEditor extends colibri.ui.ide.FileEditor {
                    constructor(language) {
                        super("phasereditor2d.core.ui.editors.JavaScriptEditor");
                        this._language = language;
                    }
                    createPart() {
                        this._monacoEditor = monaco.editor.create(this.getElement(), {
                            language: this._language
                        });
                        this.updateContent();
                    }
                    setInput(file) {
                        super.setInput(file);
                        this.updateContent();
                    }
                    async updateContent() {
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
                    onEditorInputContentChanged() {
                    }
                }
                editors.MonacoEditor = MonacoEditor;
            })(editors = ui.editors || (ui.editors = {}));
        })(ui = code.ui || (code.ui = {}));
    })(code = phasereditor2d.code || (phasereditor2d.code = {}));
})(phasereditor2d || (phasereditor2d = {}));
