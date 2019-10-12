namespace phasereditor2d.ui.ide.editors.scene.blocks {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;

    export const PREFAB_SECTION = "prefab";

    export class SceneEditorBlocksTreeRendererProvider extends pack.viewers.AssetPackBlocksTreeViewerRenderer {

        constructor(viewer: controls.viewers.TreeViewer) {
            super(viewer);

            this.setSections([

                PREFAB_SECTION,
                pack.IMAGE_TYPE,
                pack.ATLAS_TYPE,
                pack.SPRITESHEET_TYPE

            ]);
        }
    }
}