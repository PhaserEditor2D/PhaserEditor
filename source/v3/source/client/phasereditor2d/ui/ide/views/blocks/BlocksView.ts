/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.ide.blocks {
    export class BlocksView extends ide.ViewPart {
        constructor() {
            super("blocksView");
            this.setTitle("Blocks");
            this.setIcon(Workbench.getWorkbench().getWorkbenchIcon(ICON_BLOCKS));
        }
    }
}