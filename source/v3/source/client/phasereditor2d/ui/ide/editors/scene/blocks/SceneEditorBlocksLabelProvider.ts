namespace phasereditor2d.ui.ide.editors.scene.blocks {

    export class SceneEditorBlocksLabelProvider extends pack.viewers.AssetPackLabelProvider {

        getLabel(obj: any) {

            if (obj instanceof core.io.FilePath) {
                return obj.getName();
            }

            return super.getLabel(obj);
        }

    }

}