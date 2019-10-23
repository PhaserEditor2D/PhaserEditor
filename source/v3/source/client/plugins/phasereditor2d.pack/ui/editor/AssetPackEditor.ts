namespace phasereditor2d.pack.ui.editor {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;
    import dialogs = controls.dialogs;
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
        private _outlineProvider = new AssetPackEditorOutlineProvider(this);
        private _propertySectionProvider = new AssetPackEditorPropertySectionProvider();

        constructor() {
            super("phasereditor2d.AssetPackEditor");

            this.addClass("AssetPackEditor");
        }

        static getFactory(): AssetPackEditorFactory {
            return new AssetPackEditorFactory();
        }

        protected createViewer(): controls.viewers.TreeViewer {
            const viewer = new controls.viewers.TreeViewer();

            viewer.setContentProvider(new AssetPackEditorContentProvider(this, true));
            viewer.setLabelProvider(new viewers.AssetPackLabelProvider());
            viewer.setCellRendererProvider(new viewers.AssetPackCellRendererProvider("grid"));
            viewer.setTreeRenderer(new viewers.AssetPackTreeViewerRenderer(viewer, true));
            viewer.setInput(this);

            viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                this._outlineProvider.setSelection(viewer.getSelection(), true, true);
                this._outlineProvider.repaint();
            });

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

        getPropertyProvider() {
            return this._propertySectionProvider;
        }

        createEditorToolbar(parent: HTMLElement) {

            const manager = new controls.ToolbarManager(parent);

            manager.add(new controls.Action({
                text: "Add File",
                icon: ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_PLUS),
                callback: () => {
                    this.openAddFileDialog();
                }
            }));

            return manager;
        }

        private openAddFileDialog() {

            const viewer = new controls.viewers.TreeViewer();

            viewer.setLabelProvider(new viewers.AssetPackLabelProvider());
            viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
            viewer.setCellRendererProvider(new viewers.AssetPackCellRendererProvider("tree"));
            viewer.setInput(core.TYPES);

            const dlg = new dialogs.ViewerDialog(viewer);
            dlg.create();

            viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, e => {

                const type = <string>viewer.getSelection()[0];

                dlg.close();

                this.openSelectFileDialog(type);
            });
        }

        private openSelectFileDialog(type: string) {

            const viewer = new controls.viewers.TreeViewer();

            viewer.setLabelProvider(new files.ui.viewers.FileLabelProvider());
            viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
            viewer.setCellRendererProvider(new files.ui.viewers.FileCellRendererProvider());

            const folder = this.getInput().getParent();

            const importer = importers.Importers.getImporter(type);

            const list = folder.flatTree([], false)
                .filter(file => importer.acceptFile(file));

            viewer.setInput(list);

            const dlg = new dialogs.ViewerDialog(viewer);
            dlg.create();

            viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, async (e) => {
                
                const file = viewer.getSelection()[0];
                
                await importer.importFile(this._pack, file);
                
                dlg.close();

                this._viewer.repaint();
            });
        }
    }
}