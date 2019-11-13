var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ide = colibri.ui.ide;
        files.ICON_NEW_FILE = "file-new";
        files.ICON_FILE_FONT = "file-font";
        files.ICON_FILE_IMAGE = "file-image";
        files.ICON_FILE_VIDEO = "file-movie";
        files.ICON_FILE_SCRIPT = "file-script";
        files.ICON_FILE_SOUND = "file-sound";
        files.ICON_FILE_TEXT = "file-text";
        class FilesPlugin extends ide.Plugin {
            constructor() {
                super("phasereditor2d.files");
            }
            static getInstance() {
                return this._instance;
            }
            registerExtensions(reg) {
                // icons loader
                reg.addExtension(colibri.ui.ide.IconLoaderExtension.POINT_ID, colibri.ui.ide.IconLoaderExtension.withPluginFiles(this, [
                    files.ICON_FILE_IMAGE,
                    files.ICON_FILE_SOUND,
                    files.ICON_FILE_VIDEO,
                    files.ICON_FILE_SCRIPT,
                    files.ICON_FILE_TEXT,
                    files.ICON_FILE_FONT,
                    files.ICON_NEW_FILE
                ]));
                // content type resolvers
                reg.addExtension(colibri.core.ContentTypeExtension.POINT_ID, new colibri.core.ContentTypeExtension("phasereditor2d.files.core.DefaultExtensionTypeResolver", [new files.core.DefaultExtensionTypeResolver()], 1000));
                // content type icons
                reg.addExtension(ide.ContentTypeIconExtension.POINT_ID, ide.ContentTypeIconExtension.withPluginIcons(this, [
                    {
                        iconName: files.ICON_FILE_IMAGE,
                        contentType: files.core.CONTENT_TYPE_IMAGE
                    },
                    {
                        iconName: files.ICON_FILE_IMAGE,
                        contentType: files.core.CONTENT_TYPE_SVG
                    },
                    {
                        iconName: files.ICON_FILE_SOUND,
                        contentType: files.core.CONTENT_TYPE_AUDIO
                    },
                    {
                        iconName: files.ICON_FILE_VIDEO,
                        contentType: files.core.CONTENT_TYPE_VIDEO
                    },
                    {
                        iconName: files.ICON_FILE_SCRIPT,
                        contentType: files.core.CONTENT_TYPE_SCRIPT
                    },
                    {
                        iconName: files.ICON_FILE_SCRIPT,
                        contentType: files.core.CONTENT_TYPE_JAVASCRIPT
                    },
                    {
                        iconName: files.ICON_FILE_SCRIPT,
                        contentType: files.core.CONTENT_TYPE_CSS
                    },
                    {
                        iconName: files.ICON_FILE_SCRIPT,
                        contentType: files.core.CONTENT_TYPE_HTML
                    },
                    {
                        iconName: files.ICON_FILE_SCRIPT,
                        contentType: files.core.CONTENT_TYPE_XML
                    },
                    {
                        iconName: files.ICON_FILE_TEXT,
                        contentType: files.core.CONTENT_TYPE_TEXT
                    }
                ]));
                // new files
                reg.addExtension(files.ui.dialogs.NewFileExtension.POINT, new files.ui.dialogs.NewFolderExtension());
            }
        }
        FilesPlugin._instance = new FilesPlugin();
        files.FilesPlugin = FilesPlugin;
        ide.Workbench.getWorkbench().addPlugin(FilesPlugin.getInstance());
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var core;
        (function (core) {
            class ExtensionContentTypeResolver extends colibri.core.ContentTypeResolver {
                constructor(id, defs) {
                    super(id);
                    this._map = new Map();
                    for (const def of defs) {
                        this._map.set(def[0].toUpperCase(), def[1]);
                    }
                }
                computeContentType(file) {
                    const ext = file.getExtension().toUpperCase();
                    if (this._map.has(ext)) {
                        return Promise.resolve(this._map.get(ext));
                    }
                    return Promise.resolve(colibri.core.CONTENT_TYPE_ANY);
                }
            }
            core.ExtensionContentTypeResolver = ExtensionContentTypeResolver;
        })(core = files.core || (files.core = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./ExtensionContentTypeResolver.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var core;
        (function (core) {
            core.CONTENT_TYPE_IMAGE = "image";
            core.CONTENT_TYPE_SVG = "svg";
            core.CONTENT_TYPE_AUDIO = "audio";
            core.CONTENT_TYPE_VIDEO = "video";
            core.CONTENT_TYPE_SCRIPT = "script";
            core.CONTENT_TYPE_TEXT = "text";
            core.CONTENT_TYPE_CSV = "csv";
            core.CONTENT_TYPE_JAVASCRIPT = "javascript";
            core.CONTENT_TYPE_HTML = "html";
            core.CONTENT_TYPE_CSS = "css";
            core.CONTENT_TYPE_JSON = "json";
            core.CONTENT_TYPE_XML = "xml";
            core.CONTENT_TYPE_GLSL = "glsl";
            class DefaultExtensionTypeResolver extends core.ExtensionContentTypeResolver {
                constructor() {
                    super("phasereditor2d.files.core.DefaultExtensionTypeResolver", [
                        ["png", core.CONTENT_TYPE_IMAGE],
                        ["jpg", core.CONTENT_TYPE_IMAGE],
                        ["bmp", core.CONTENT_TYPE_IMAGE],
                        ["gif", core.CONTENT_TYPE_IMAGE],
                        ["webp", core.CONTENT_TYPE_IMAGE],
                        ["svg", core.CONTENT_TYPE_SVG],
                        ["mp3", core.CONTENT_TYPE_AUDIO],
                        ["wav", core.CONTENT_TYPE_AUDIO],
                        ["ogg", core.CONTENT_TYPE_AUDIO],
                        ["mp4", core.CONTENT_TYPE_VIDEO],
                        ["ogv", core.CONTENT_TYPE_VIDEO],
                        ["mp4", core.CONTENT_TYPE_VIDEO],
                        ["webm", core.CONTENT_TYPE_VIDEO],
                        ["js", core.CONTENT_TYPE_JAVASCRIPT],
                        ["html", core.CONTENT_TYPE_HTML],
                        ["css", core.CONTENT_TYPE_CSS],
                        ["ts", core.CONTENT_TYPE_SCRIPT],
                        ["json", core.CONTENT_TYPE_JSON],
                        ["xml", core.CONTENT_TYPE_XML],
                        ["glsl", core.CONTENT_TYPE_GLSL],
                        ["txt", core.CONTENT_TYPE_TEXT],
                        ["md", core.CONTENT_TYPE_TEXT],
                        ["csv", core.CONTENT_TYPE_CSV]
                    ]);
                }
            }
            core.DefaultExtensionTypeResolver = DefaultExtensionTypeResolver;
        })(core = files.core || (files.core = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var actions;
            (function (actions) {
                var controls = colibri.ui.controls;
                class OpenNewFileDialogAction extends controls.Action {
                    constructor() {
                        super({
                            text: "New",
                            icon: files.FilesPlugin.getInstance().getIcon(files.ICON_NEW_FILE)
                        });
                    }
                    run() {
                        const viewer = new controls.viewers.TreeViewer();
                        viewer.setLabelProvider(new WizardLabelProvider());
                        viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
                        viewer.setCellRendererProvider(new WizardCellRendererProvider());
                        const extensions = colibri.ui.ide.Workbench.getWorkbench()
                            .getExtensionRegistry()
                            .getExtensions(files.ui.dialogs.NewFileExtension.POINT);
                        viewer.setInput(extensions);
                        const dlg = new controls.dialogs.ViewerDialog(viewer);
                        dlg.create();
                        dlg.setTitle("New");
                        {
                            const selectCallback = () => {
                                dlg.close();
                                this.openFileDialog(viewer.getSelectionFirstElement());
                            };
                            const btn = dlg.addButton("Select", () => selectCallback());
                            btn.disabled = true;
                            viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                                btn.disabled = viewer.getSelection().length !== 1;
                            });
                            viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, e => selectCallback());
                        }
                        dlg.addButton("Cancel", () => dlg.close());
                    }
                    openFileDialog(extension) {
                        var _a;
                        const dlg = extension.createDialog();
                        dlg.setTitle(`New ${extension.getWizardName()}`);
                        dlg.setInitialFileName(extension.getInitialFileName());
                        dlg.setInitialLocation((_a = this._initialLocation, (_a !== null && _a !== void 0 ? _a : extension.getInitialFileLocation())));
                        dlg.validate();
                    }
                    setInitialLocation(folder) {
                        this._initialLocation = folder;
                    }
                }
                actions.OpenNewFileDialogAction = OpenNewFileDialogAction;
                class WizardLabelProvider {
                    getLabel(obj) {
                        return obj.getWizardName();
                    }
                }
                class WizardCellRendererProvider {
                    getCellRenderer(element) {
                        const ext = element;
                        return new controls.viewers.IconImageCellRenderer(ext.getIcon());
                    }
                    preload(element) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                }
            })(actions = ui.actions || (ui.actions = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var dialogs;
            (function (dialogs) {
                var controls = colibri.ui.controls;
                var viewers = colibri.ui.controls.viewers;
                class BaseNewFileDialog extends controls.dialogs.Dialog {
                    constructor() {
                        super("NewFileDialog");
                    }
                    createDialogArea() {
                        const clientArea = document.createElement("div");
                        clientArea.classList.add("DialogClientArea");
                        clientArea.style.display = "grid";
                        clientArea.style.gridTemplateRows = "1fr auto";
                        clientArea.style.gridTemplateRows = "1fr";
                        clientArea.style.gridRowGap = "5px";
                        clientArea.appendChild(this.createCenterArea());
                        clientArea.appendChild(this.createBottomArea());
                        this.getElement().appendChild(clientArea);
                    }
                    createBottomArea() {
                        const bottomArea = document.createElement("div");
                        bottomArea.classList.add("DialogSection");
                        bottomArea.style.display = "grid";
                        bottomArea.style.gridTemplateColumns = "auto 1fr";
                        bottomArea.style.gridTemplateRows = "auto";
                        bottomArea.style.columnGap = "10px";
                        bottomArea.style.rowGap = "10px";
                        bottomArea.style.alignItems = "center";
                        {
                            const label = document.createElement("label");
                            label.innerText = "Location";
                            bottomArea.appendChild(label);
                            const text = document.createElement("input");
                            text.type = "text";
                            text.readOnly = true;
                            bottomArea.appendChild(text);
                            this._filteredViewer.getViewer().addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                                const file = this._filteredViewer.getViewer().getSelectionFirstElement();
                                text.value = file === null ? "" : `${file.getFullName()}/`;
                            });
                        }
                        {
                            const label = document.createElement("label");
                            label.innerText = "Name";
                            bottomArea.appendChild(label);
                            const text = document.createElement("input");
                            text.type = "text";
                            bottomArea.appendChild(text);
                            setTimeout(() => text.focus(), 10);
                            text.addEventListener("keyup", e => this.validate());
                            this._fileNameText = text;
                        }
                        return bottomArea;
                    }
                    normalizedFileName() {
                        return this._fileNameText.value;
                    }
                    validate() {
                        const folder = this._filteredViewer.getViewer().getSelectionFirstElement();
                        let valid = folder !== null;
                        if (valid) {
                            const name = this.normalizedFileName();
                            if (name.indexOf("/") >= 0 || name.trim() === "") {
                                valid = false;
                            }
                            else {
                                const file = folder.getFile(name);
                                if (file) {
                                    valid = false;
                                }
                            }
                        }
                        this._createBtn.disabled = !valid;
                    }
                    setFileCreatedCallback(callback) {
                        this._fileCreatedCallback = callback;
                    }
                    setInitialFileName(filename) {
                        this._fileNameText.value = filename;
                    }
                    setInitialLocation(folder) {
                        this._filteredViewer.getViewer().setSelection([folder]);
                        this._filteredViewer.getViewer().reveal(folder);
                    }
                    create() {
                        super.create();
                        this._createBtn = this.addButton("Create", () => this.createFile_priv());
                        this.addButton("Cancel", () => this.close());
                        this.validate();
                    }
                    async createFile_priv() {
                        const folder = this._filteredViewer.getViewer().getSelectionFirstElement();
                        const name = this.normalizedFileName();
                        const file = await this.createFile(folder, name);
                        this.close();
                        if (this._fileCreatedCallback) {
                            this._fileCreatedCallback(file);
                        }
                    }
                    createCenterArea() {
                        const centerArea = document.createElement("div");
                        this.createFilteredViewer();
                        centerArea.appendChild(this._filteredViewer.getElement());
                        return centerArea;
                    }
                    createFilteredViewer() {
                        const viewer = new viewers.TreeViewer();
                        viewer.setLabelProvider(new files.ui.viewers.FileLabelProvider());
                        viewer.setContentProvider(new files.ui.viewers.FileTreeContentProvider(true));
                        viewer.setCellRendererProvider(new files.ui.viewers.FileCellRendererProvider());
                        viewer.setInput(colibri.ui.ide.Workbench.getWorkbench().getProjectRoot());
                        viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                            this.validate();
                        });
                        this._filteredViewer = new viewers.FilteredViewerInElement(viewer);
                    }
                    layout() {
                        super.layout();
                        this._filteredViewer.resizeTo();
                    }
                }
                dialogs.BaseNewFileDialog = BaseNewFileDialog;
            })(dialogs = ui.dialogs || (ui.dialogs = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files_1) {
        var ui;
        (function (ui) {
            var dialogs;
            (function (dialogs) {
                class NewFileExtension extends colibri.core.extensions.Extension {
                    constructor(config) {
                        super(config.id);
                        this._wizardName = config.wizardName;
                        this._icon = config.icon;
                        this._initialFileName = config.initialFileName;
                    }
                    getInitialFileName() {
                        return this._initialFileName;
                    }
                    getWizardName() {
                        return this._wizardName;
                    }
                    getIcon() {
                        return this._icon;
                    }
                    getInitialFileLocation() {
                        return colibri.ui.ide.Workbench.getWorkbench().getProjectRoot();
                    }
                    findInitialFileLocationBasedOnContentType(contentType) {
                        const root = colibri.ui.ide.Workbench.getWorkbench().getProjectRoot();
                        const files = [];
                        root.flatTree(files, false);
                        const reg = colibri.ui.ide.Workbench.getWorkbench().getContentTypeRegistry();
                        const targetFiles = files.filter(file => contentType === reg.getCachedContentType(file));
                        if (targetFiles.length > 0) {
                            targetFiles.sort((a, b) => {
                                return b.getModTime() - a.getModTime();
                            });
                            return targetFiles[0].getParent();
                        }
                        return root;
                    }
                }
                NewFileExtension.POINT = "phasereditor2d.files.ui.dialogs.NewFileDialogExtension";
                dialogs.NewFileExtension = NewFileExtension;
            })(dialogs = ui.dialogs || (ui.dialogs = {}));
        })(ui = files_1.ui || (files_1.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./NewFileExtension.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var dialogs;
            (function (dialogs) {
                class NewFileContentExtension extends dialogs.NewFileExtension {
                    constructor(config) {
                        super(config);
                        this._fileExtension = config.fileExtension;
                        this._fileContent = config.fileContent;
                    }
                    createDialog() {
                        const dlg = new files.ui.dialogs.NewFileDialog();
                        dlg.create();
                        dlg.setFileExtension(this._fileExtension);
                        dlg.setFileContent(this._fileContent);
                        dlg.setFileCreatedCallback(async (file) => {
                            const wb = colibri.ui.ide.Workbench.getWorkbench();
                            const reg = wb.getContentTypeRegistry();
                            await reg.preload(file);
                            wb.openEditor(file);
                        });
                        return dlg;
                    }
                }
                dialogs.NewFileContentExtension = NewFileContentExtension;
            })(dialogs = ui.dialogs || (ui.dialogs = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseNewFileDialog.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var dialogs;
            (function (dialogs) {
                class NewFileDialog extends dialogs.BaseNewFileDialog {
                    constructor() {
                        super();
                        this._fileExtension = "";
                        this._fileContent = "";
                    }
                    normalizedFileName() {
                        const name = super.normalizedFileName();
                        if (name.endsWith("." + this._fileExtension)) {
                            return name;
                        }
                        return name + "." + this._fileExtension;
                    }
                    setFileContent(fileContent) {
                        this._fileContent = fileContent;
                    }
                    setFileExtension(fileExtension) {
                        this._fileExtension = fileExtension;
                    }
                    createFile(folder, name) {
                        return colibri.ui.ide.FileUtils.createFile_async(folder, name, this._fileContent);
                    }
                }
                dialogs.NewFileDialog = NewFileDialog;
            })(dialogs = ui.dialogs || (ui.dialogs = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var dialogs;
            (function (dialogs) {
                class NewFolderDialog extends dialogs.BaseNewFileDialog {
                    createFile(container, name) {
                        return colibri.ui.ide.FileUtils.createFolder_async(container, name);
                    }
                }
                dialogs.NewFolderDialog = NewFolderDialog;
            })(dialogs = ui.dialogs || (ui.dialogs = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var dialogs;
            (function (dialogs) {
                class NewFolderExtension extends dialogs.NewFileExtension {
                    constructor() {
                        super({
                            id: "phasereditor2d.files.ui.dialogs.NewFolderExtension",
                            icon: colibri.ui.ide.Workbench.getWorkbench().getWorkbenchIcon(colibri.ui.ide.ICON_FOLDER),
                            initialFileName: "folder",
                            wizardName: "Folder"
                        });
                    }
                    createDialog() {
                        const dlg = new dialogs.NewFolderDialog();
                        dlg.create();
                        return dlg;
                    }
                }
                dialogs.NewFolderExtension = NewFolderExtension;
            })(dialogs = ui.dialogs || (ui.dialogs = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                class ContentTypeCellRendererExtension extends colibri.core.extensions.Extension {
                    constructor(id) {
                        super(id);
                    }
                }
                ContentTypeCellRendererExtension.POINT = "phasereditor2d.files.ui.viewers.ContentTypeCellRendererExtension";
                viewers.ContentTypeCellRendererExtension = ContentTypeCellRendererExtension;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers_1) {
                var viewers = colibri.ui.controls.viewers;
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                class FileCellRenderer extends viewers.IconImageCellRenderer {
                    constructor() {
                        super(null);
                    }
                    getIcon(obj) {
                        const file = obj;
                        if (file.isFile()) {
                            const ct = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);
                            const icon = ide.Workbench.getWorkbench().getContentTypeIcon(ct);
                            if (icon) {
                                return icon;
                            }
                        }
                        else {
                            return controls.Controls.getIcon(ide.ICON_FOLDER);
                        }
                        return controls.Controls.getIcon(ide.ICON_FILE);
                    }
                    preload(obj) {
                        const file = obj;
                        if (file.isFile()) {
                            return ide.Workbench.getWorkbench().getContentTypeRegistry().preload(file);
                        }
                        return super.preload(obj);
                    }
                }
                viewers_1.FileCellRenderer = FileCellRenderer;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers_2) {
                var ide = colibri.ui.ide;
                class FileCellRendererProvider {
                    constructor(layout = "tree") {
                        this._layout = layout;
                    }
                    getCellRenderer(file) {
                        const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);
                        const extensions = ide.Workbench
                            .getWorkbench()
                            .getExtensionRegistry()
                            .getExtensions(viewers_2.ContentTypeCellRendererExtension.POINT);
                        for (const extension of extensions) {
                            const provider = extension.getRendererProvider(contentType);
                            if (provider !== null) {
                                return provider.getCellRenderer(file);
                            }
                        }
                        return new viewers_2.FileCellRenderer();
                    }
                    preload(file) {
                        return ide.Workbench.getWorkbench().getContentTypeRegistry().preload(file);
                    }
                }
                viewers_2.FileCellRendererProvider = FileCellRendererProvider;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers_3) {
                class FileLabelProvider {
                    getLabel(obj) {
                        return obj.getName();
                    }
                }
                viewers_3.FileLabelProvider = FileLabelProvider;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files_2) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                var core = colibri.core;
                class FileTreeContentProvider {
                    constructor(onlyFolders = false) {
                        this._onlyFolders = onlyFolders;
                    }
                    getRoots(input) {
                        let result = [];
                        if (input instanceof core.io.FilePath) {
                            if (this._onlyFolders) {
                                if (!input.isFolder()) {
                                    return [];
                                }
                            }
                            return [input];
                        }
                        if (input instanceof Array) {
                            if (this._onlyFolders) {
                                return input.filter(f => f.isFolder());
                            }
                            return input;
                        }
                        return this.getChildren(input);
                    }
                    getChildren(parent) {
                        const files = parent.getFiles();
                        if (this._onlyFolders) {
                            return files.filter(f => f.isFolder());
                        }
                        return files;
                    }
                }
                viewers.FileTreeContentProvider = FileTreeContentProvider;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = files_2.ui || (files_2.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                var controls = colibri.ui.controls;
                class Provider {
                    constructor(_renderer) {
                        this._renderer = _renderer;
                    }
                    getCellRenderer(element) {
                        return this._renderer;
                    }
                    preload(element) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                }
                class SimpleContentTypeCellRendererExtension extends colibri.core.extensions.Extension {
                    constructor(contentType, cellRenderer) {
                        super("phasereditor2d.files.ui.viewers.SimpleContentTypeCellRendererExtension");
                        this._contentType = contentType;
                        this._cellRenderer = cellRenderer;
                    }
                    getRendererProvider(contentType) {
                        if (contentType === this._contentType) {
                            return new Provider(this._cellRenderer);
                        }
                        return null;
                    }
                }
                viewers.SimpleContentTypeCellRendererExtension = SimpleContentTypeCellRendererExtension;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var views;
            (function (views) {
                var controls = colibri.ui.controls;
                class FilePropertySectionProvider extends controls.properties.PropertySectionProvider {
                    addSections(page, sections) {
                        sections.push(new views.FileSection(page));
                        sections.push(new views.ImageFileSection(page));
                        sections.push(new views.ManyImageFileSection(page));
                    }
                }
                views.FilePropertySectionProvider = FilePropertySectionProvider;
            })(views = ui.views || (ui.views = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var views;
            (function (views) {
                var controls = colibri.ui.controls;
                var core = colibri.core;
                class FileSection extends controls.properties.PropertySection {
                    constructor(page) {
                        super(page, "files.FileSection", "File");
                    }
                    createForm(parent) {
                        const comp = this.createGridElement(parent, 2);
                        {
                            // Name
                            this.createLabel(comp, "Name");
                            const text = this.createText(comp, true);
                            this.addUpdater(() => {
                                text.value = this.flatValues_StringJoin(this.getSelection().map(file => file.getName()));
                            });
                        }
                        {
                            // Full Name
                            this.createLabel(comp, "Full Name");
                            const text = this.createText(comp, true);
                            this.addUpdater(() => {
                                text.value = this.flatValues_StringJoin(this.getSelection().map(file => file.getFullName()));
                            });
                        }
                        {
                            // Size
                            this.createLabel(comp, "Size");
                            const text = this.createText(comp, true);
                            this.addUpdater(() => {
                                let total = 0;
                                for (const file of this.getSelection()) {
                                    total += file.getSize();
                                }
                                text.value = total.toString();
                            });
                        }
                    }
                    canEdit(obj) {
                        return obj instanceof core.io.FilePath;
                    }
                    canEditNumber(n) {
                        return n > 0;
                    }
                }
                views.FileSection = FileSection;
            })(views = ui.views || (ui.views = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files_3) {
        var ui;
        (function (ui) {
            var views;
            (function (views) {
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                var io = colibri.core.io;
                class FilesView extends ide.ViewerView {
                    constructor() {
                        super("filesView");
                        this._propertyProvider = new views.FilePropertySectionProvider();
                        this.setTitle("Files");
                        this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FOLDER));
                    }
                    createViewer() {
                        return new controls.viewers.TreeViewer();
                    }
                    fillContextMenu(menu) {
                        const sel = this._viewer.getSelection();
                        menu.add(new controls.Action({
                            text: "New...",
                            enabled: sel.length === 1,
                            callback: () => this.onNewFile()
                        }));
                        menu.add(new controls.Action({
                            text: "Rename",
                            enabled: sel.length === 1,
                            callback: () => this.onRenameFile()
                        }));
                        menu.add(new controls.Action({
                            text: "Move",
                            enabled: sel.length > 0
                        }));
                        menu.add(new controls.Action({
                            text: "Delete",
                            enabled: sel.length > 0,
                            callback: () => {
                                const files = this._viewer.getSelection();
                                if (confirm(`Do you want to delete ${files.length} files?`)) {
                                    if (files.length > 0) {
                                        ide.FileUtils.deleteFiles_async(files);
                                    }
                                }
                            }
                        }));
                    }
                    onNewFile() {
                        const action = new ui.actions.OpenNewFileDialogAction();
                        let folder = this._viewer.getSelectionFirstElement();
                        if (folder) {
                            if (folder.isFile()) {
                                folder = folder.getParent();
                            }
                            action.setInitialLocation(folder);
                        }
                        action.run();
                    }
                    onRenameFile() {
                        const file = this._viewer.getSelectionFirstElement();
                        const parent = file.getParent();
                        const dlg = new controls.dialogs.InputDialog();
                        dlg.create();
                        dlg.setTitle("Rename");
                        dlg.setMessage("Enter the new name");
                        dlg.setInitialValue(file.getName());
                        dlg.setInputValidator(value => {
                            var _a;
                            if (value.indexOf("/") >= 0) {
                                return false;
                            }
                            if (parent) {
                                const file2 = (_a = parent.getFile(value), (_a !== null && _a !== void 0 ? _a : null));
                                return file2 === null;
                            }
                            return false;
                        });
                        dlg.setResultCallback(result => {
                            ide.FileUtils.renameFile_async(file, result);
                        });
                        dlg.validate();
                    }
                    getPropertyProvider() {
                        return this._propertyProvider;
                    }
                    createPart() {
                        super.createPart();
                        const wb = ide.Workbench.getWorkbench();
                        const root = wb.getProjectRoot();
                        const viewer = this._viewer;
                        viewer.setLabelProvider(new ui.viewers.FileLabelProvider());
                        viewer.setContentProvider(new ui.viewers.FileTreeContentProvider());
                        viewer.setCellRendererProvider(new ui.viewers.FileCellRendererProvider());
                        viewer.setInput(root);
                        viewer.repaint();
                        viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, (e) => {
                            wb.openEditor(e.detail);
                        });
                        wb.getFileStorage().addChangeListener(change => {
                            viewer.setInput(ide.FileUtils.getRoot());
                            viewer.repaint();
                            if (change.getAddedFiles().length === 1) {
                                const file = change.getAddedFiles()[0];
                                if (file.isFolder()) {
                                    setTimeout(() => {
                                        viewer.reveal(file);
                                        viewer.setSelection([file]);
                                    }, 100);
                                }
                            }
                        });
                        wb.addEventListener(ide.EVENT_EDITOR_ACTIVATED, e => {
                            const editor = wb.getActiveEditor();
                            if (editor) {
                                const input = editor.getInput();
                                if (input instanceof io.FilePath) {
                                    viewer.setSelection([input]);
                                    viewer.reveal(input);
                                }
                            }
                        });
                    }
                    getIcon() {
                        return controls.Controls.getIcon(ide.ICON_FOLDER);
                    }
                }
                views.FilesView = FilesView;
            })(views = ui.views || (ui.views = {}));
        })(ui = files_3.ui || (files_3.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var views;
            (function (views) {
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                var core = colibri.core;
                class ImageFileSection extends controls.properties.PropertySection {
                    constructor(page) {
                        super(page, "files.ImagePreviewSection", "Image", true);
                    }
                    createForm(parent) {
                        parent.classList.add("ImagePreviewFormArea", "PreviewBackground");
                        const imgControl = new controls.ImageControl(ide.IMG_SECTION_PADDING);
                        this.getPage().addEventListener(controls.EVENT_CONTROL_LAYOUT, (e) => {
                            imgControl.resizeTo();
                        });
                        parent.appendChild(imgControl.getElement());
                        setTimeout(() => imgControl.resizeTo(), 1);
                        this.addUpdater(() => {
                            const file = this.getSelection()[0];
                            const img = ide.Workbench.getWorkbench().getFileImage(file);
                            imgControl.setImage(img);
                            setTimeout(() => imgControl.resizeTo(), 1);
                        });
                    }
                    canEdit(obj) {
                        if (obj instanceof core.io.FilePath) {
                            const ct = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(obj);
                            return ct === files.core.CONTENT_TYPE_IMAGE || ct === files.core.CONTENT_TYPE_SVG;
                        }
                        return false;
                    }
                    canEditNumber(n) {
                        return n == 1;
                    }
                }
                views.ImageFileSection = ImageFileSection;
            })(views = ui.views || (ui.views = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var files;
    (function (files) {
        var ui;
        (function (ui) {
            var views;
            (function (views) {
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                var core = colibri.core;
                class GridImageFileViewer extends controls.viewers.TreeViewer {
                    constructor(...classList) {
                        super("PreviewBackground", ...classList);
                        this.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
                        this.setLabelProvider(new ui.viewers.FileLabelProvider());
                        this.setCellRendererProvider(new ui.viewers.FileCellRendererProvider());
                        this.setTreeRenderer(new controls.viewers.GridTreeViewerRenderer(this, false, true));
                        this.getCanvas().classList.add("PreviewBackground");
                    }
                }
                class ManyImageFileSection extends controls.properties.PropertySection {
                    constructor(page) {
                        super(page, "files.ManyImageFileSection", "Images", true);
                    }
                    createForm(parent) {
                        parent.classList.add("ManyImagePreviewFormArea");
                        const viewer = new GridImageFileViewer();
                        const filteredViewer = new ide.properties.FilteredViewerInPropertySection(this.getPage(), viewer);
                        parent.appendChild(filteredViewer.getElement());
                        this.addUpdater(() => {
                            // clean the viewer first
                            viewer.setInput([]);
                            viewer.repaint();
                            viewer.setInput(this.getSelection());
                            filteredViewer.resizeTo();
                        });
                    }
                    canEdit(obj) {
                        if (obj instanceof core.io.FilePath) {
                            const ct = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(obj);
                            return ct === files.core.CONTENT_TYPE_IMAGE || ct === files.core.CONTENT_TYPE_SVG;
                        }
                        return false;
                    }
                    canEditNumber(n) {
                        return n > 1;
                    }
                }
                views.ManyImageFileSection = ManyImageFileSection;
            })(views = ui.views || (ui.views = {}));
        })(ui = files.ui || (files.ui = {}));
    })(files = phasereditor2d.files || (phasereditor2d.files = {}));
})(phasereditor2d || (phasereditor2d = {}));
