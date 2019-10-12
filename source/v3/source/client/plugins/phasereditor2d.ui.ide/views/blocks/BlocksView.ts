
namespace phasereditor2d.ui.ide.views.blocks {

    import viewers = controls.viewers;
    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;

    export class BlocksView extends ide.EditorViewerView {

        static EDITOR_VIEWER_PROVIDER_KEY = "Blocks";

        constructor() {
            super("BlocksView");

            this.setTitle("Blocks");
            this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_BLOCKS));

        }

        getViewerProvider(editor: ide.EditorPart) {
            return editor.getEditorViewerProvider(BlocksView.EDITOR_VIEWER_PROVIDER_KEY);
        }
    }
}