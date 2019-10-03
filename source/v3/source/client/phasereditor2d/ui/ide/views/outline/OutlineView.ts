/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.ide.views.outline {

    export class OutlineView extends ide.EditorViewerView {

        static EDITOR_VIEWER_PROVIDER_KEY = "Outline";

        constructor() {
            super("OutlineView");

            this.setTitle("Outline");
            this.setIcon(Workbench.getWorkbench().getWorkbenchIcon(ICON_OUTLINE));
        }

        getViewerProvider(editor: EditorPart): EditorViewerProvider {
            return editor.getEditorViewerProvider(OutlineView.EDITOR_VIEWER_PROVIDER_KEY);
        }
    }
}