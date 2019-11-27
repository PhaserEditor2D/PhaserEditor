var phasereditor2d;
(function (phasereditor2d) {
    var code;
    (function (code) {
        var controls = colibri.ui.controls;
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
                // project preloaders
                this._modelsManager = new code.ui.editors.MonacoModelsManager();
                // reg.addExtension(colibri.ui.ide.PreloadProjectResourcesExtension.POINT_ID,
                //     new colibri.ui.ide.PreloadProjectResourcesExtension("phasereditor2d.code.ui.editors.EditorModelsManager",
                //         monitor => {
                //             return this._modelsManager.start(monitor);
                //         }));
                // editors
                reg.addExtension(colibri.ui.ide.EditorExtension.POINT_ID, new colibri.ui.ide.EditorExtension("phasereditor2d.core.ui.editors", [
                    new code.ui.editors.JavaScriptEditorFactory(),
                    new code.ui.editors.MonacoEditorFactory("typescript", phasereditor2d.webContentTypes.core.CONTENT_TYPE_SCRIPT),
                    new code.ui.editors.MonacoEditorFactory("html", phasereditor2d.webContentTypes.core.CONTENT_TYPE_HTML),
                    new code.ui.editors.MonacoEditorFactory("css", phasereditor2d.webContentTypes.core.CONTENT_TYPE_CSS),
                    new code.ui.editors.MonacoEditorFactory("json", phasereditor2d.webContentTypes.core.CONTENT_TYPE_JSON),
                    new code.ui.editors.MonacoEditorFactory("xml", phasereditor2d.webContentTypes.core.CONTENT_TYPE_XML),
                    new code.ui.editors.MonacoEditorFactory("text", phasereditor2d.webContentTypes.core.CONTENT_TYPE_TEXT),
                ]));
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
                    constructor(language, contentType) {
                        super("phasereditor2d.core.ui.editors.MonacoEditorFactory#" + language);
                        this._language = language;
                        this._contentType = contentType;
                    }
                    acceptInput(input) {
                        if (input instanceof io.FilePath) {
                            const contentType = colibri.ui.ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                            return this._contentType === contentType;
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
                        this.addClass("MonacoEditor");
                        this._language = language;
                    }
                    getMonacoEditor() {
                        return this._monacoEditor;
                    }
                    onPartClosed() {
                        if (this._monacoEditor) {
                            this._monacoEditor.dispose();
                        }
                        return super.onPartClosed();
                    }
                    createPart() {
                        const container = document.createElement("div");
                        container.classList.add("MonacoEditorContainer");
                        this.getElement().appendChild(container);
                        this._monacoEditor = this.createMonacoEditor(container);
                        this._monacoEditor.onDidChangeModelContent(e => {
                            this.setDirty(true);
                        });
                        editors.MonacoModelsManager.getInstance().start();
                    }
                    createMonacoEditor(container) {
                        return monaco.editor.create(container, this.createMonacoEditorOptions());
                    }
                    createMonacoEditorOptions() {
                        return {
                            language: this._language,
                            fontSize: 16,
                            scrollBeyondLastLine: false
                        };
                    }
                    async doSave() {
                        const content = this._monacoEditor.getValue();
                        try {
                            await colibri.ui.ide.FileUtils.setFileString_async(this.getInput(), content);
                            this.setDirty(false);
                        }
                        catch (e) {
                            console.error(e);
                        }
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
                        this.setDirty(false);
                    }
                    layout() {
                        super.layout();
                        if (this._monacoEditor) {
                            this._monacoEditor.layout();
                        }
                    }
                    onEditorInputContentChanged() {
                    }
                }
                editors.MonacoEditor = MonacoEditor;
            })(editors = ui.editors || (ui.editors = {}));
        })(ui = code.ui || (code.ui = {}));
    })(code = phasereditor2d.code || (phasereditor2d.code = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./MonacoEditor.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var code;
    (function (code) {
        var ui;
        (function (ui) {
            var editors;
            (function (editors) {
                class JavaScriptEditorFactory extends editors.MonacoEditorFactory {
                    constructor() {
                        super("javascript", phasereditor2d.webContentTypes.core.CONTENT_TYPE_JAVASCRIPT);
                    }
                    createEditor() {
                        return new JavaScriptEditor();
                    }
                }
                editors.JavaScriptEditorFactory = JavaScriptEditorFactory;
                class JavaScriptEditor extends editors.MonacoEditor {
                    constructor() {
                        super("javascript");
                    }
                }
                editors.JavaScriptEditor = JavaScriptEditor;
            })(editors = ui.editors || (ui.editors = {}));
        })(ui = code.ui || (code.ui = {}));
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
                class MonacoModelsManager {
                    constructor() {
                        this._started = false;
                        this._extraLibs = [];
                    }
                    static getInstance() {
                        if (!this._instance) {
                            this._instance = new MonacoModelsManager();
                        }
                        return this._instance;
                    }
                    async start() {
                        if (this._started) {
                            return;
                        }
                        this._started = true;
                        this.updateExtraLibs();
                        colibri.ui.ide.Workbench.getWorkbench().getFileStorage().addChangeListener(change => this.onStorageChanged(change));
                    }
                    onStorageChanged(change) {
                        console.info(`MonacoModelsManager: storage changed.`);
                        const test = (paths) => {
                            for (const r of paths) {
                                if (r.endsWith(".d.ts")) {
                                    return true;
                                }
                            }
                            return false;
                        };
                        const update = test(change.getDeleteRecords())
                            || test(change.getModifiedRecords())
                            || test(change.getRenameToRecords())
                            || test(change.getRenameFromRecords())
                            || test(change.getAddRecords());
                        if (update) {
                            this.updateExtraLibs();
                        }
                    }
                    async updateExtraLibs() {
                        console.log("MonacoModelsManager: updating extra libs.");
                        for (const lib of this._extraLibs) {
                            console.log(`MonacoModelsManager: disposing ${lib.file.getFullName()}`);
                            lib.disposable.dispose();
                        }
                        const files = colibri.ui.ide.FileUtils.getRoot().flatTree([], false);
                        const tsFiles = files.filter(file => file.getName().endsWith(".d.ts"));
                        for (const file of tsFiles) {
                            const content = await colibri.ui.ide.FileUtils.preloadAndGetFileString(file);
                            const d = monaco.languages.typescript.javascriptDefaults.addExtraLib(content);
                            console.log(`MonacoModelsManager: add extra lib ${file.getFullName()}`);
                            this._extraLibs.push({
                                disposable: d,
                                file: file
                            });
                        }
                    }
                }
                editors.MonacoModelsManager = MonacoModelsManager;
            })(editors = ui.editors || (ui.editors = {}));
        })(ui = code.ui || (code.ui = {}));
    })(code = phasereditor2d.code || (phasereditor2d.code = {}));
})(phasereditor2d || (phasereditor2d = {}));
