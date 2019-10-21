declare namespace phasereditor2d.inspector {
    import ide = colibri.ui.ide;
    const ICON_INSPECTOR = "inspector";
    class InspectorPlugin extends ide.Plugin {
        private static _instance;
        static getInstance(): InspectorPlugin;
        private constructor();
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
    }
}
declare namespace phasereditor2d.inspector.ui.views {
    import ide = colibri.ui.ide;
    class InspectorView extends ide.ViewPart {
        private _propertyPage;
        private _currentPart;
        private _selectionListener;
        constructor();
        layout(): void;
        protected createPart(): void;
        private onWorkbenchPartActivate;
        private onPartSelection;
    }
}
//# sourceMappingURL=plugin.d.ts.map