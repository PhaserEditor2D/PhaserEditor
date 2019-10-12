
namespace phasereditor2d.scene.ui.blocks {

    export class SceneEditorBlocksCellRendererProvider extends pack.ui.viewers.AssetPackCellRendererProvider {

        getCellRenderer(element: any) {

            if (element instanceof colibri.core.io.FilePath) {

                return new SceneCellRenderer();

            }

            return super.getCellRenderer(element);
        }

    }

}