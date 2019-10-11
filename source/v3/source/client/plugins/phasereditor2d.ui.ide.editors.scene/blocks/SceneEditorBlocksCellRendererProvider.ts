namespace phasereditor2d.ui.ide.editors.scene.blocks {

    export class SceneEditorBlocksCellRendererProvider extends pack.viewers.AssetPackCellRendererProvider {

        getCellRenderer(element: any) {

            if (element instanceof core.io.FilePath) {

                return new SceneCellRenderer();

            }

            return super.getCellRenderer(element);
        }

    }

}