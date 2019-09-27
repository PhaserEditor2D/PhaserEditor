/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.ide.outline {

    export class OutlineView extends ide.ViewPart {
        
        constructor() {
            super("outlineView");
            this.setTitle("Outline");
            this.setIcon(Workbench.getWorkbench().getWorkbenchIcon(ICON_OUTLINE));
        }

        protected createPart(): void {
            
        }
    }
}