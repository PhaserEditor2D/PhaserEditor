namespace phasereditor2d.pack.ui.editor {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export class AssetPackEditorBlocksProvider extends ide.EditorViewerProvider {

        private _editor: AssetPackEditor;

        constructor(editor: AssetPackEditor) {
            super();

            this._editor = editor;
        }

        getContentProvider(): colibri.ui.controls.viewers.ITreeContentProvider {
            return new files.ui.viewers.FileTreeContentProvider();
        }

        getLabelProvider(): colibri.ui.controls.viewers.ILabelProvider {
            return new files.ui.viewers.FileLabelProvider();
        }

        getCellRendererProvider(): colibri.ui.controls.viewers.ICellRendererProvider {
            return new files.ui.viewers.FileCellRendererProvider("grid");
        }

        getTreeViewerRenderer(viewer: colibri.ui.controls.viewers.TreeViewer): colibri.ui.controls.viewers.TreeViewerRenderer {
            return new AssetPackEditorTreeViewerRenderer(this._editor, viewer);
        }

        getPropertySectionProvider(): colibri.ui.controls.properties.PropertySectionProvider {
            return new AssetPackEditorPropertySectionProvider();
        }

        getInput() {
            return this._editor.getInput().getParent().getFiles();
        }

        preload(): Promise<void> {
            return Promise.resolve();
        }
    }
}