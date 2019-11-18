declare namespace phasereditor2d.ide.ui {
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
        createParts(): void;
        private initToolbar;
        getEditorArea(): ide.EditorArea;
        private initialLayout;
    }
}
declare namespace phasereditor2d.ide {
    import ide = colibri.ui.ide;
    class IDEPlugin extends ide.Plugin {
        private static _instance;
        static getInstance(): IDEPlugin;
        private constructor();
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
    }
    const VER = "3.0.0";
}
//# sourceMappingURL=plugin.d.ts.map