declare namespace phasereditor2d.outline {
    import ide = colibri.ui.ide;
    const ICON_OUTLINE = "outline";
    class OutlinePlugin extends ide.Plugin {
        private static _instance;
        static getInstance(): OutlinePlugin;
        constructor();
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
    }
}
declare namespace phasereditor2d.outline.ui.views {
    import ide = colibri.ui.ide;
    class OutlineView extends ide.EditorViewerView {
        static EDITOR_VIEWER_PROVIDER_KEY: string;
        constructor();
        getViewerProvider(editor: ide.EditorPart): ide.EditorViewerProvider;
    }
}
//# sourceMappingURL=plugin.d.ts.map