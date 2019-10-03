namespace phasereditor2d.ui.ide.editors.scene {

    class SceneEditorOutlineLabelProvider implements controls.viewers.ILabelProvider {

        getLabel(obj: any): string {

            if (obj instanceof Phaser.GameObjects.GameObject) {
                return obj.getEditorLabel();
            }

            if (obj instanceof Phaser.GameObjects.DisplayList) {
                return "Display List";
            }

            return "" + obj;
        }

    }

    class SceneEditorOutlineContentProvider implements controls.viewers.ITreeContentProvider {

        getRoots(input: any): any[] {

            const editor: SceneEditor = input;

            return [editor.getGameScene().sys.displayList];
        }

        getChildren(parent: any): any[] {

            if (parent instanceof Phaser.GameObjects.DisplayList) {
                return parent.getChildren();
            }

            return [];
        }

    }

    class SceneEditorOutlineRendererProvider implements controls.viewers.ICellRendererProvider {

        getCellRenderer(element: any): controls.viewers.ICellRenderer {
           return new controls.viewers.EmptyCellRenderer();
        }        
        
        preload(element: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }


    }

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
            return new SceneEditorOutlineRendererProvider();
        }

        getTreeViewerRenderer(viewer: controls.viewers.TreeViewer): controls.viewers.TreeViewerRenderer {
            return new controls.viewers.TreeViewerRenderer(viewer);
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
    }

}