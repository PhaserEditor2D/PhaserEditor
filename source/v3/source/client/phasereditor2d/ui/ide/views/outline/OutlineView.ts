/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.ide.views.outline {

    export class OutlineView extends ide.ViewerView {

        constructor() {
            super("OutlineView");
            this.setTitle("Outline");
            this.setIcon(Workbench.getWorkbench().getWorkbenchIcon(ICON_OUTLINE));
        }

        protected createViewer(): controls.viewers.TreeViewer {
            return new controls.viewers.TreeViewer();
        }
    }
}