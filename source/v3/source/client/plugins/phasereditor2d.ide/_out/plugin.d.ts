declare namespace phasereditor2d.ide {
    import ide = colibri.ui.ide;
    const ICON_NEW_FILE = "file-new";
    class IDEPlugin extends ide.Plugin {
        private static _instance;
        static getInstance(): IDEPlugin;
        private constructor();
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
        createWindow(windows: ide.WorkbenchWindow[]): void;
        openNewWizard(): void;
    }
    const VER = "3.0.0";
}
declare namespace phasereditor2d.ide.ui.actions {
    import controls = colibri.ui.controls;
    class OpenNewFileDialogAction extends controls.Action {
        constructor();
        run(): void;
        private openFileDialog;
    }
}
declare namespace phasereditor2d.ide.ui.dialogs {
    import controls = colibri.ui.controls;
    import io = colibri.core.io;
    type CreateFileCallback = (folder: io.FilePath, filename: string) => void;
    class NewFileDialog extends controls.dialogs.Dialog {
        private _filteredViewer;
        private _fileNameText;
        private _createBtn;
        private _fileExtension;
        private _fileContent;
        private _fileCreatedCallback;
        constructor();
        protected createDialogArea(): void;
        private createBottomArea;
        private normalizedFileName;
        validate(): void;
        setFileCreatedCallback(callback: (file: io.FilePath) => void): void;
        setFileContent(fileContent: string): void;
        setInitialFileName(filename: string): void;
        setFileExtension(fileExtension: string): void;
        setInitialLocation(folder: io.FilePath): void;
        create(): void;
        private createFile;
        private createCenterArea;
        private createFilteredViewer;
        layout(): void;
    }
}
declare namespace phasereditor2d.ide.ui.dialogs {
    import controls = colibri.ui.controls;
    import io = colibri.core.io;
    class NewFileDialogExtension extends colibri.core.extensions.Extension {
        static POINT: string;
        private _wizardName;
        private _icon;
        private _initialFileName;
        private _fileExtension;
        private _fileContent;
        constructor(config: {
            id: string;
            wizardName: string;
            icon: controls.IImage;
            initialFileName: string;
            fileExtension: string;
            fileContent: string;
        });
        getFileContent(): string;
        getInitialFileName(): string;
        getFileExtension(): string;
        getWizardName(): string;
        getIcon(): controls.IImage;
        getInitialFileLocation(): io.FilePath;
        findInitialFileLocationBasedOnContentType(contentType: string): io.FilePath;
    }
}
declare namespace phasereditor2d.ide.ui.windows {
    import ide = colibri.ui.ide;
    class DesignWindow extends ide.WorkbenchWindow {
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
        private initToolbar;
        getEditorArea(): ide.EditorArea;
        private initialLayout;
    }
}
//# sourceMappingURL=plugin.d.ts.map