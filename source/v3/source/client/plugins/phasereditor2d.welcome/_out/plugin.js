var phasereditor2d;
(function (phasereditor2d) {
    var welcome;
    (function (welcome) {
        class WelcomePlugin extends colibri.ui.ide.Plugin {
            constructor() {
                super("phasereditor2d.welcome");
            }
            static getInstance() {
                if (!this._instance) {
                    this._instance = new WelcomePlugin();
                }
                return this._instance;
            }
            createWindow(windows) {
                windows.push(new welcome.ui.WelcomeWindow());
            }
            registerExtensions(reg) {
                // windows
                reg.addExtension(colibri.ui.ide.WindowExtension.ID, new colibri.ui.ide.WindowExtension("phasereditor2d.ide.ui.WelcomeWindow", 5, () => new welcome.ui.WelcomeWindow()));
            }
        }
        welcome.WelcomePlugin = WelcomePlugin;
        colibri.ui.ide.Workbench.getWorkbench().addPlugin(WelcomePlugin.getInstance());
    })(welcome = phasereditor2d.welcome || (phasereditor2d.welcome = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var welcome;
    (function (welcome) {
        var ui;
        (function (ui) {
            var controls = colibri.ui.controls;
            class WelcomeWindow extends colibri.ui.ide.WorkbenchWindow {
                constructor() {
                    super("phasereditor2d.welcome.ui.WelcomeWindow");
                }
                getEditorArea() {
                    return new colibri.ui.ide.EditorArea();
                }
                createParts() {
                    const dlg = new controls.dialogs.Dialog("WelcomeDialog");
                    dlg.create();
                    dlg.setTitle("Projects");
                    dlg.addButton("Open Project", () => { });
                    dlg.addButton("New Project", () => { });
                }
            }
            ui.WelcomeWindow = WelcomeWindow;
        })(ui = welcome.ui || (welcome.ui = {}));
    })(welcome = phasereditor2d.welcome || (phasereditor2d.welcome = {}));
})(phasereditor2d || (phasereditor2d = {}));
