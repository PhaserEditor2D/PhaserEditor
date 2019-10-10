namespace phasereditor2d.ui.ide.editors.scene.outline {

    export class SceneEditorOutlineProvider extends ide.EditorViewerProvider {

        private _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            super();
            this._editor = editor;
        }

        getContentProvider(): controls.viewers.ITreeContentProvider {
            return new SceneEditorOutlineContentProvider();
        }

        getLabelProvider(): controls.viewers.ILabelProvider {
            return new SceneEditorOutlineLabelProvider();
        }

        getCellRendererProvider(): controls.viewers.ICellRendererProvider {
            return new SceneEditorOutlineRendererProvider(this._editor);
        }

        getTreeViewerRenderer(viewer: controls.viewers.TreeViewer): controls.viewers.TreeViewerRenderer {
            return new controls.viewers.TreeViewerRenderer(viewer, 48);
        }

        getPropertySectionProvider(): controls.properties.PropertySectionProvider {
            return this._editor.getPropertyProvider();
        }

        getInput() {
            return this._editor;
        }

        preload(): Promise<void> {
            return;
        }

        onViewerSelectionChanged(selection: any[]) {
            this._editor.setSelection(selection, false);
            this._editor.repaint();
        }
    }

}