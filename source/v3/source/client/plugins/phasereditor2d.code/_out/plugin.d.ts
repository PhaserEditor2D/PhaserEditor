declare namespace phasereditor2d.code {
    class CodePlugin extends colibri.ui.ide.Plugin {
        private static _instance;
        private _modelsManager;
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
        protected getMonacoEditor(): monaco.editor.IStandaloneCodeEditor;
        onPartClosed(): boolean;
        protected createPart(): void;
        private getTokensAtLine;
        protected createMonacoEditor(container: HTMLElement): monaco.editor.IStandaloneCodeEditor;
        protected createMonacoEditorOptions(): monaco.editor.IEditorConstructionOptions;
        doSave(): Promise<void>;
        setInput(file: io.FilePath): void;
        private updateContent;
        layout(): void;
        protected onEditorInputContentChanged(): void;
    }
}
declare namespace phasereditor2d.code.ui.editors {
    class JavaScriptEditorFactory extends MonacoEditorFactory {
        constructor();
        createEditor(): colibri.ui.ide.EditorPart;
    }
    class JavaScriptEditor extends MonacoEditor {
        constructor();
        createPart(): void;
    }
}
declare namespace phasereditor2d.code.ui.editors {
    class MonacoModelsManager {
        private static _instance;
        static getInstance(): MonacoModelsManager;
        private _started;
        private _extraLibs;
        constructor();
        start(): Promise<void>;
        private onStorageChanged;
        updateExtraLibs(): Promise<void>;
    }
}
//# sourceMappingURL=plugin.d.ts.map