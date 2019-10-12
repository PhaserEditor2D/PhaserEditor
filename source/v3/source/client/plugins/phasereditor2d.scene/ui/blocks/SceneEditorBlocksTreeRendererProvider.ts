namespace phasereditor2d.scene.ui.blocks {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;

    export const PREFAB_SECTION = "prefab";

    export class SceneEditorBlocksTreeRendererProvider extends pack.ui.viewers.AssetPackBlocksTreeViewerRenderer {

        constructor(viewer: controls.viewers.TreeViewer) {
            super(viewer);

            this.setSections([

                PREFAB_SECTION,
                pack.core.IMAGE_TYPE,
                pack.core.ATLAS_TYPE,
                pack.core.SPRITESHEET_TYPE

            ]);
        }
    }
}