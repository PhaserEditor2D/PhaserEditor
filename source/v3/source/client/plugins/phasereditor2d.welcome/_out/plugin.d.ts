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
        protected createParts(): Promise<void>;
    }
}
declare namespace phasereditor2d.welcome.ui.dialogs {
    import controls = colibri.ui.controls;
    class ProjectsDialog extends controls.dialogs.ViewerDialog {
        constructor();
        create(): Promise<void>;
        private openProject;
    }
}
declare namespace phasereditor2d.welcome.ui.viewers {
    import controls = colibri.ui.controls;
    class ProjectCellRendererProvider implements controls.viewers.ICellRendererProvider {
        getCellRenderer(element: any): controls.viewers.ICellRenderer;
        preload(element: any): Promise<controls.PreloadResult>;
    }
}
//# sourceMappingURL=plugin.d.ts.map