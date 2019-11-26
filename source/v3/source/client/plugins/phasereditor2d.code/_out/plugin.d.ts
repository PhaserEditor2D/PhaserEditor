declare namespace phasereditor2d.code {
    class CodePlugin extends colibri.ui.ide.Plugin {
        private static _instance;
        static getInstance(): CodePlugin;
        constructor();
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
        starting(): Promise<void>;
    }
}
declare namespace phasereditor2d.code.ui.editors {
    import io = colibri.core.io;
    class MonacoEditorFactory extends colibri.ui.ide.EditorFactory {
        private _language;
        private _contentType;
        constructor(language: string, contentType: string);
        acceptInput(input: any): boolean;
        createEditor(): colibri.ui.ide.EditorPart;
    }
    class MonacoEditor extends colibri.ui.ide.FileEditor {
        private static _factory;
        private _monacoEditor;
        private _language;
        constructor(language: string);
        protected createPart(): void;
        setInput(file: io.FilePath): void;
        private updateContent;
        layout(): void;
        protected onEditorInputContentChanged(): void;
    }
}
//# sourceMappingURL=plugin.d.ts.map