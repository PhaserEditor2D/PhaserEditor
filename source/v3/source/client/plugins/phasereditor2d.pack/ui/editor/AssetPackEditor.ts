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

        constructor() {
            super("phasereditor2d.AssetPackEditor");
            this.addClass("AssetPackEditor");
        }

        static getFactory(): AssetPackEditorFactory {
            return new AssetPackEditorFactory();
        }

        protected createViewer(): controls.viewers.TreeViewer {
            const viewer = new controls.viewers.TreeViewer();

            viewer.setContentProvider(new AssetPackEditorContentProvider());
            viewer.setLabelProvider(new viewers.AssetPackLabelProvider());
            viewer.setCellRendererProvider(new viewers.AssetPackCellRendererProvider());
            viewer.setTreeRenderer(new viewers.AssetPackTreeViewerRenderer(viewer, true));

            this.updateContent();

            return viewer;
        }

        private async updateContent() {
            const file = this.getInput();

            if (!file) {
                return;
            }

            const content = await ide.FileUtils.preloadAndGetFileString(file);
            const pack = new core.AssetPack(file, content);

            this.getViewer().setContentProvider(new AssetPackEditorContentProvider(pack));
            this.getViewer().setInput(pack);

            this.getViewer().repaint();
        }

        setInput(file: io.FilePath): void {
            super.setInput(file);
            this.updateContent();
        }
    }
}