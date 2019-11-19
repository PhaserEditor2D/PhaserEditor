var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide_1) {
        var ui;
        (function (ui) {
            var controls = colibri.ui.controls;
            var ide = colibri.ui.ide;
            class DesignWindow extends ide.WorkbenchWindow {
                constructor() {
                    super(DesignWindow.ID);
                }
                createParts() {
                    this._outlineView = new phasereditor2d.outline.ui.views.OutlineView();
                    this._filesView = new phasereditor2d.files.ui.views.FilesView();
                    this._inspectorView = new phasereditor2d.inspector.ui.views.InspectorView();
                    this._blocksView = new phasereditor2d.blocks.ui.views.BlocksView();
                    this._editorArea = new ide.EditorArea();
                    this._split_Files_Blocks = new controls.SplitPanel(this.createViewFolder(this._filesView), this.createViewFolder(this._blocksView));
                    this._split_Editor_FilesBlocks = new controls.SplitPanel(this._editorArea, this._split_Files_Blocks, false);
                    this._split_Outline_EditorFilesBlocks = new controls.SplitPanel(this.createViewFolder(this._outlineView), this._split_Editor_FilesBlocks);
                    this._split_OutlineEditorFilesBlocks_Inspector = new controls.SplitPanel(this._split_Outline_EditorFilesBlocks, this.createViewFolder(this._inspectorView));
                    this.getClientArea().add(this._split_OutlineEditorFilesBlocks_Inspector);
                    this.initToolbar();
                    this.initialLayout();
                }
                initToolbar() {
                    const toolbar = this.getToolbar();
                    const leftArea = toolbar.getLeftArea();
                    const manager = new controls.ToolbarManager(leftArea);
                    manager.add(new phasereditor2d.files.ui.actions.OpenNewFileDialogAction());
                    manager.add(new phasereditor2d.welcome.ui.actions.OpenProjectsDialogAction());
                    manager.add(new phasereditor2d.ide.ui.actions.PlayProjectAction());
                }
                getEditorArea() {
                    return this._editorArea;
                }
                initialLayout() {
                    //const b = { x: 0, y: 0, width: window.innerWidth, height: window.innerHeight };
                    this._split_Files_Blocks.setSplitFactor(0.2);
                    this._split_Editor_FilesBlocks.setSplitFactor(0.6);
                    this._split_Outline_EditorFilesBlocks.setSplitFactor(0.15);
                    this._split_OutlineEditorFilesBlocks_Inspector.setSplitFactor(0.8);
                    //this.setBounds(b);
                    this.layout();
                }
            }
            DesignWindow.ID = "phasereditor2d.ide.ui.DesignWindow";
            ui.DesignWindow = DesignWindow;
        })(ui = ide_1.ui || (ide_1.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide_2) {
        var ide = colibri.ui.ide;
        ide_2.ICON_PLAY = "play";
        class IDEPlugin extends ide.Plugin {
            constructor() {
                super("phasereditor2d.ide");
            }
            static getInstance() {
                return this._instance;
            }
            registerExtensions(reg) {
                // windows
                reg.addExtension(colibri.ui.ide.WindowExtension.POINT_ID, new colibri.ui.ide.WindowExtension("phasereditor2d.ide.ui.DesignWindow", 10, () => new ide_2.ui.DesignWindow()));
                // icons
                reg.addExtension(colibri.ui.ide.IconLoaderExtension.POINT_ID, new colibri.ui.ide.IconLoaderExtension("phasereditor2d.ide.ui.IconLoader", [
                    this.getIcon(ide_2.ICON_PLAY)
                ]));
            }
        }
        IDEPlugin._instance = new IDEPlugin();
        ide_2.IDEPlugin = IDEPlugin;
        ide.Workbench.getWorkbench().addPlugin(IDEPlugin.getInstance());
        ide_2.VER = "3.0.0";
        function main() {
            console.log(`%c %c Phaser Editor 2D %c v${ide_2.VER} %c %c https://phasereditor2d.com `, "background-color:red", "background-color:#3f3f3f;color:whitesmoke", "background-color:orange;color:black", "background-color:red", "background-color:silver");
            ide.Workbench.getWorkbench().launch();
        }
        window.addEventListener("load", main);
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide) {
        var ui;
        (function (ui) {
            var actions;
            (function (actions) {
                var controls = colibri.ui.controls;
                class PlayProjectAction extends controls.Action {
                    constructor() {
                        super({
                            text: "Play Project",
                            icon: ide.IDEPlugin.getInstance().getIcon(ide.ICON_PLAY)
                        });
                    }
                    run() {
                        const element = document.createElement("a");
                        element.href = colibri.ui.ide.FileUtils.getRoot().getUrl();
                        element.target = "blank";
                        document.body.append(element);
                        element.click();
                        element.remove();
                    }
                }
                actions.PlayProjectAction = PlayProjectAction;
            })(actions = ui.actions || (ui.actions = {}));
        })(ui = ide.ui || (ide.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
