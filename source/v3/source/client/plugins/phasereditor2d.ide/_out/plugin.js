var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide_1) {
        var ide = colibri.ui.ide;
        class IDEPlugin extends ide.Plugin {
            constructor() {
                super("phasereditor2d.ide");
            }
            static getInstance() {
                return this._instance;
            }
            createWindow(windows) {
                windows.push(new ide_1.ui.windows.DesignWindow());
            }
        }
        IDEPlugin._instance = new IDEPlugin();
        ide_1.IDEPlugin = IDEPlugin;
        ide.Workbench.getWorkbench().addPlugin(IDEPlugin.getInstance());
        ide_1.VER = "3.0.0";
        function main() {
            console.log(`%c %c Phaser Editor 2D %c v${ide_1.VER} %c %c https://phasereditor2d.com `, "background-color:red", "background-color:#3f3f3f;color:whitesmoke", "background-color:orange;color:black", "background-color:red", "background-color:silver");
            ide.Workbench.getWorkbench().launch();
        }
        window.addEventListener("load", main);
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide_2) {
        var ui;
        (function (ui) {
            var windows;
            (function (windows) {
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                class DesignWindow extends ide.WorkbenchWindow {
                    constructor() {
                        super();
                        this._outlineView = new phasereditor2d.outline.ui.views.OutlineView();
                        this._filesView = new phasereditor2d.files.ui.views.FilesView();
                        this._inspectorView = new phasereditor2d.inspector.ui.views.InspectorView();
                        this._blocksView = new phasereditor2d.blocks.ui.views.BlocksView();
                        this._editorArea = new ide.EditorArea();
                        this._split_Files_Blocks = new controls.SplitPanel(this.createViewFolder(this._filesView), this.createViewFolder(this._blocksView));
                        this._split_Editor_FilesBlocks = new controls.SplitPanel(this._editorArea, this._split_Files_Blocks, false);
                        this._split_Outline_EditorFilesBlocks = new controls.SplitPanel(this.createViewFolder(this._outlineView), this._split_Editor_FilesBlocks);
                        this._split_OutlineEditorFilesBlocks_Inspector = new controls.SplitPanel(this._split_Outline_EditorFilesBlocks, this.createViewFolder(this._inspectorView));
                        this.add(this._split_OutlineEditorFilesBlocks_Inspector);
                        this.initialLayout();
                    }
                    getEditorArea() {
                        return this._editorArea;
                    }
                    initialLayout() {
                        const b = { x: 0, y: 0, width: window.innerWidth, height: window.innerHeight };
                        this._split_Files_Blocks.setSplitFactor(0.2);
                        this._split_Editor_FilesBlocks.setSplitFactor(0.6);
                        this._split_Outline_EditorFilesBlocks.setSplitFactor(0.15);
                        this._split_OutlineEditorFilesBlocks_Inspector.setSplitFactor(0.8);
                        this.setBounds(b);
                    }
                }
                windows.DesignWindow = DesignWindow;
            })(windows = ui.windows || (ui.windows = {}));
        })(ui = ide_2.ui || (ide_2.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
