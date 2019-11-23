declare namespace phasereditor2d.code {
    class CodePlugin extends colibri.ui.ide.Plugin {
        private static _instance;
        static getInstance(): CodePlugin;
        constructor();
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
    }
}
declare namespace phasereditor2d.code.ui.editors {
    class JavaScriptEditorFactory extends colibri.ui.ide.EditorFactory {
        constructor();
        acceptInput(input: any): boolean;
        createEditor(): colibri.ui.ide.EditorPart;
    }
    class JavaScriptEditor extends colibri.ui.ide.EditorPart {
        private static _factory;
        static getFactory(): colibri.ui.ide.EditorFactory;
        constructor();
        protected createPart(): void;
    }
}
//# sourceMappingURL=plugin.d.ts.map