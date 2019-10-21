namespace phasereditor2d.pack.ui.editor {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;
    import io = colibri.core.io;

    export class AssetPackEditorFactory extends ide.EditorFactory {

        constructor() {
            super("phasereditor2d.AssetPackEditorFactory");
        }

        acceptInput(input: any): boolean {
            if (input instanceof io.FilePath) {
                const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                return contentType === pack.core.CONTENT_TYPE_ASSET_PACK;
            }
            return false;
        }

        createEditor(): ide.EditorPart {
            return new AssetPackEditor();
        }

    }

    export class AssetPackEditor extends ide.ViewerFileEditor {
        private _pack: core.AssetPack;
        private _contentProvider : AssetPackEditorContentProvider;
        private _outlineProvider = new AssetPackEditorOutlineProvider(this);

        constructor() {
            super("phasereditor2d.AssetPackEditor");

            this.addClass("AssetPackEditor");
        }

        static getFactory(): AssetPackEditorFactory {
            return new AssetPackEditorFactory();
        }

        protected createViewer(): controls.viewers.TreeViewer {
            const viewer = new controls.viewers.TreeViewer();

            viewer.setContentProvider(this._contentProvider = new AssetPackEditorContentProvider(this));
            viewer.setLabelProvider(new viewers.AssetPackLabelProvider());
            viewer.setCellRendererProvider(new viewers.AssetPackCellRendererProvider("grid"));
            viewer.setTreeRenderer(new viewers.AssetPackTreeViewerRenderer(viewer, true));
            viewer.setInput(this);

            this.updateContent();

            return viewer;
        }

        private async updateContent() {
            const file = this.getInput();

            if (!file) {
                return;
            }

            const content = await ide.FileUtils.preloadAndGetFileString(file);
            this._pack = new core.AssetPack(file, content);

            this.getViewer().repaint();

            this._outlineProvider.repaint();
        }

        getPack() {
            return this._pack;
        }

        setInput(file: io.FilePath): void {
            super.setInput(file);
            this.updateContent();
        }

        getEditorViewerProvider(key: string): ide.EditorViewerProvider {

            switch (key) {
                case outline.ui.views.OutlineView.EDITOR_VIEWER_PROVIDER_KEY:
                    return this._outlineProvider;
            }

            return null;
        }
    }
}