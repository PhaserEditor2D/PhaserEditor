/// <reference path="../../phasereditor2d.pack/ui/viewers/AssetPackCellRendererProvider.ts" />

namespace phasereditor2d.ui.ide.editors.scene.blocks {

    export class SceneEditorBlocksCellRendererProvider extends pack.ui.viewers.AssetPackCellRendererProvider {

        getCellRenderer(element: any) {

            if (element instanceof colibri.core.io.FilePath) {

                return new SceneCellRenderer();

            }

            return super.getCellRenderer(element);
        }

    }

}