declare namespace phasereditor2d.ide {
    import ide = colibri.ui.ide;
    const ICON_PLAY = "play";
    class IDEPlugin extends ide.Plugin {
        private static _instance;
        private _openingProject;
        static getInstance(): IDEPlugin;
        private constructor();
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
        openFirstWindow(): Promise<void>;
        ideOpenProject(projectName: string): Promise<void>;
        isOpeningProject(): boolean;
        switchTheme(): void;
        restoreTheme(): void;
    }
    const VER = "3.0.0";
}
declare namespace phasereditor2d.ide.ui {
    import ide = colibri.ui.ide;
    class DesignWindow extends ide.WorkbenchWindow {
        static ID: string;
        private _outlineView;
        private _filesView;
        private _inspectorView;
        private _blocksView;
        private _editorArea;
        private _split_Files_Blocks;
        private _split_Editor_FilesBlocks;
        private _split_Outline_EditorFilesBlocks;
        private _split_OutlineEditorFilesBlocks_Inspector;
        constructor();
        saveState(prefs: colibri.core.preferences.Preferences): void;
        restoreState(prefs: colibri.core.preferences.Preferences): void;
        createParts(): void;
        private initToolbar;
        getEditorArea(): ide.EditorArea;
        private initialLayout;
    }
}
declare namespace phasereditor2d.ide.ui {
    class WelcomeWindow extends colibri.ui.ide.WorkbenchWindow {
        static ID: string;
        constructor();
        getEditorArea(): colibri.ui.ide.EditorArea;
        protected createParts(): Promise<void>;
    }
}
declare namespace phasereditor2d.ide.ui.actions {
    const CMD_OPEN_PROJECTS_DIALOG = "phasereditor2d.ide.ui.actions.OpenProjectsDialog";
    const CMD_SWITCH_THEME = "phasereditor2d.ide.ui.actions.SwitchTheme";
    class IDEActions {
        static registerCommands(manager: colibri.ui.ide.commands.CommandManager): void;
    }
}
declare namespace phasereditor2d.ide.ui.actions {
    import controls = colibri.ui.controls;
    class OpenProjectsDialogAction extends controls.Action {
        constructor();
        run(): void;
    }
}
declare namespace phasereditor2d.ide.ui.actions {
    import controls = colibri.ui.controls;
    class PlayProjectAction extends controls.Action {
        constructor();
        run(): void;
    }
}
declare namespace phasereditor2d.ide.ui.dialogs {
    import controls = colibri.ui.controls;
    class OpeningProjectDialog extends controls.dialogs.ProgressDialog {
        create(): void;
    }
}
declare namespace phasereditor2d.ide.ui.dialogs {
    import controls = colibri.ui.controls;
    class ProjectsDialog extends controls.dialogs.ViewerDialog {
        constructor();
        create(): Promise<void>;
        private openProject;
    }
}
declare namespace phasereditor2d.ide.ui.viewers {
    import controls = colibri.ui.controls;
    class ProjectCellRendererProvider implements controls.viewers.ICellRendererProvider {
        getCellRenderer(element: any): controls.viewers.ICellRenderer;
        preload(element: any): Promise<controls.PreloadResult>;
    }
}
//# sourceMappingURL=plugin.d.ts.map