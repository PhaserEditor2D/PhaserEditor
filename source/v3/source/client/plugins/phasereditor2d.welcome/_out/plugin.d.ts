declare namespace phasereditor2d.welcome {
    class WelcomePlugin extends colibri.ui.ide.Plugin {
        private static _instance;
        static getInstance(): WelcomePlugin;
        constructor();
        createWindow(windows: colibri.ui.ide.WorkbenchWindow[]): void;
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
    }
}
declare namespace phasereditor2d.welcome.ui {
    class WelcomeWindow extends colibri.ui.ide.WorkbenchWindow {
        constructor();
        getEditorArea(): colibri.ui.ide.EditorArea;
        protected createParts(): void;
    }
}
//# sourceMappingURL=plugin.d.ts.map