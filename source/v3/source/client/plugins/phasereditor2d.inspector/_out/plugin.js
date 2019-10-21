var phasereditor2d;
(function (phasereditor2d) {
    var inspector;
    (function (inspector) {
        var ide = colibri.ui.ide;
        inspector.ICON_INSPECTOR = "inspector";
        class InspectorPlugin extends ide.Plugin {
            constructor() {
                super("phasereditor2d.inspector");
            }
            static getInstance() {
                return this._instance;
            }
            registerExtensions(reg) {
                reg.addExtension(ide.CSSFileLoaderExtension.POINT_ID, new ide.CSSFileLoaderExtension("phasereditor2d.inspector.CSSFileLoaderExtension", [
                    "plugins/phasereditor2d.inspector/ui/css/InspectorView.css"
                ]));
                reg.addExtension(ide.CSSFileLoaderExtension.POINT_ID, new ide.CSSFileLoaderExtension("phasereditor2d.images.ui.CSSFileLoaderExtension", [
                    "plugins/phasereditor2d.images/ui/css/ImageEditor.css",
                    "plugins/phasereditor2d.images/ui/css/ImageEditor-dark.css",
                    "plugins/phasereditor2d.images/ui/css/ImageEditor-light.css"
                ]));
                reg.addExtension(ide.IconLoaderExtension.POINT_ID, ide.IconLoaderExtension.withPluginFiles(this, [
                    inspector.ICON_INSPECTOR
                ]));
            }
        }
        InspectorPlugin._instance = new InspectorPlugin();
        inspector.InspectorPlugin = InspectorPlugin;
    })(inspector = phasereditor2d.inspector || (phasereditor2d.inspector = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var inspector;
    (function (inspector) {
        var ui;
        (function (ui) {
            var views;
            (function (views) {
                var controls = colibri.ui.controls;
                var ide = colibri.ui.ide;
                class InspectorView extends ide.ViewPart {
                    constructor() {
                        super("InspectorView");
                        this.setTitle("Inspector");
                        this.setIcon(inspector.InspectorPlugin.getInstance().getIcon(inspector.ICON_INSPECTOR));
                    }
                    layout() {
                        this._propertyPage.dispatchLayoutEvent();
                    }
                    createPart() {
                        this._propertyPage = new controls.properties.PropertyPage();
                        this.add(this._propertyPage);
                        this._selectionListener = (e) => this.onPartSelection();
                        ide.Workbench.getWorkbench().addEventListener(ide.EVENT_PART_ACTIVATED, e => this.onWorkbenchPartActivate());
                    }
                    onWorkbenchPartActivate() {
                        const part = ide.Workbench.getWorkbench().getActivePart();
                        if (part !== this && part !== this._currentPart) {
                            if (this._currentPart) {
                                this._currentPart.removeEventListener(controls.EVENT_SELECTION_CHANGED, this._selectionListener);
                            }
                            this._currentPart = part;
                            if (part) {
                                part.addEventListener(controls.EVENT_SELECTION_CHANGED, this._selectionListener);
                                this.onPartSelection();
                            }
                            else {
                                this._propertyPage.setSectionProvider(null);
                            }
                        }
                    }
                    onPartSelection() {
                        const sel = this._currentPart.getSelection();
                        const provider = this._currentPart.getPropertyProvider();
                        this._propertyPage.setSectionProvider(provider);
                        this._propertyPage.setSelection(sel);
                    }
                }
                views.InspectorView = InspectorView;
            })(views = ui.views || (ui.views = {}));
        })(ui = inspector.ui || (inspector.ui = {}));
    })(inspector = phasereditor2d.inspector || (phasereditor2d.inspector = {}));
})(phasereditor2d || (phasereditor2d = {}));
