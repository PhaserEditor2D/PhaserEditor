/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>
/// <reference path="../../EditorViewerView.ts"/>

namespace phasereditor2d.ui.ide.views.blocks {

    import viewers = controls.viewers;

    export class BlocksView extends ide.EditorViewerView {

        constructor() {
            super("BlocksView");

            this.setTitle("Blocks");
            this.setIcon(Workbench.getWorkbench().getWorkbenchIcon(ICON_BLOCKS));

        }

        getViewerProvider(editor: EditorPart) {
            return editor.getEditorViewerProvider("Blocks");
        }
    }
}