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
                reg.addExtension(colibri.ui.ide.WindowExtension.POINT_ID, new colibri.ui.ide.WindowExtension("phasereditor2d.ide.ui.WelcomeWindow", 5, () => new welcome.ui.WelcomeWindow()));
                // keys
                reg.addExtension(colibri.ui.ide.commands.CommandExtension.POINT_ID, new colibri.ui.ide.commands.CommandExtension("phasereditor2d.welcome.ui.actions.WelcomeActions", welcome.ui.actions.WelcomeActions.registerCommands));
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
            class WelcomeWindow extends colibri.ui.ide.WorkbenchWindow {
                constructor() {
                    super("phasereditor2d.welcome.ui.WelcomeWindow");
                }
                getEditorArea() {
                    return new colibri.ui.ide.EditorArea();
                }
                async createParts() {
                    const dlg = new ui.dialogs.ProjectsDialog();
                    dlg.create();
                }
            }
            ui.WelcomeWindow = WelcomeWindow;
        })(ui = welcome.ui || (welcome.ui = {}));
    })(welcome = phasereditor2d.welcome || (phasereditor2d.welcome = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var welcome;
    (function (welcome) {
        var ui;
        (function (ui) {
            var actions;
            (function (actions) {
                actions.CMD_OPEN_PROJECTS_DIALOG = "phasereditor2d.welcome.ui.actions.OpenProjectsDialog";
                class WelcomeActions {
                    static registerCommands(manager) {
                        manager.addCommandHelper(actions.CMD_OPEN_PROJECTS_DIALOG);
                        manager.addHandlerHelper(actions.CMD_OPEN_PROJECTS_DIALOG, args => {
                            console.log(args.activeWindow);
                            return args.activeWindow instanceof phasereditor2d.ide.ui.DesignWindow;
                        }, args => WelcomeActions.openProjectsDialog());
                        manager.addKeyBinding(actions.CMD_OPEN_PROJECTS_DIALOG, new colibri.ui.ide.commands.KeyMatcher({
                            control: true,
                            alt: true,
                            key: "P",
                            filterInputElements: false
                        }));
                    }
                    static openProjectsDialog() {
                        const dlg = new ui.dialogs.ProjectsDialog();
                        dlg.create();
                    }
                }
                actions.WelcomeActions = WelcomeActions;
            })(actions = ui.actions || (ui.actions = {}));
        })(ui = welcome.ui || (welcome.ui = {}));
    })(welcome = phasereditor2d.welcome || (phasereditor2d.welcome = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var welcome;
    (function (welcome) {
        var ui;
        (function (ui) {
            var dialogs;
            (function (dialogs) {
                var controls = colibri.ui.controls;
                class ProjectsDialog extends controls.dialogs.ViewerDialog {
                    constructor() {
                        super(new controls.viewers.TreeViewer());
                    }
                    async create() {
                        super.create();
                        const viewer = this.getViewer();
                        viewer.setLabelProvider(new controls.viewers.LabelProvider());
                        viewer.setCellRendererProvider(new ui.viewers.ProjectCellRendererProvider());
                        viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
                        viewer.setInput([]);
                        const activeWindow = colibri.ui.ide.Workbench.getWorkbench().getActiveWindow();
                        this.setCloseWithEscapeKey(!(activeWindow instanceof ui.WelcomeWindow));
                        this.setTitle("Projects");
                        this.addButton("New Project", () => { });
                        {
                            const btn = this.addButton("Open Project", () => this.openProject(viewer.getSelectionFirstElement()));
                            btn.disabled = true;
                            viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                                btn.disabled = !(viewer.getSelection().length === 1);
                            });
                        }
                        let projects = await colibri.ui.ide.FileUtils.getProjects_async();
                        const root = colibri.ui.ide.FileUtils.getRoot();
                        if (root) {
                            projects = projects.filter(project => root.getName() !== project);
                        }
                        viewer.setInput(projects);
                        viewer.repaint();
                    }
                    async openProject(project) {
                        const wb = colibri.ui.ide.Workbench.getWorkbench();
                        await wb.openProject(project);
                        this.close();
                        wb.activateWindow(phasereditor2d.ide.ui.DesignWindow.ID);
                    }
                }
                dialogs.ProjectsDialog = ProjectsDialog;
            })(dialogs = ui.dialogs || (ui.dialogs = {}));
        })(ui = welcome.ui || (welcome.ui = {}));
    })(welcome = phasereditor2d.welcome || (phasereditor2d.welcome = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var welcome;
    (function (welcome) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                var controls = colibri.ui.controls;
                class ProjectCellRendererProvider {
                    getCellRenderer(element) {
                        return new controls.viewers.IconImageCellRenderer(colibri.ui.ide.Workbench.getWorkbench().getWorkbenchIcon(colibri.ui.ide.ICON_FOLDER));
                    }
                    preload(element) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                }
                viewers.ProjectCellRendererProvider = ProjectCellRendererProvider;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = welcome.ui || (welcome.ui = {}));
    })(welcome = phasereditor2d.welcome || (phasereditor2d.welcome = {}));
})(phasereditor2d || (phasereditor2d = {}));
