/// <reference path="./BaseSection.ts" />

namespace phasereditor2d.pack.ui.editor.properties {

    import controls = colibri.ui.controls;
    import json = colibri.core.json;

    export class SpritesheetSection extends BaseSection {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "phasereditor2d.pack.ui.editor.properties.SpritesheetSection", "Spritesheet");
        }

        canEdit(obj: any, n: number) {
            return super.canEdit(obj, n) && obj instanceof core.SpritesheetAssetPackItem;
        }

        protected createForm(parent: HTMLDivElement) {

            const comp = this.createGridElement(parent, 3);

            comp.style.gridTemplateColumns = "auto 1fr auto";

            this.createFileField(comp, "URL", "url", core.contentTypes.CONTENT_TYPE_MULTI_ATLAS);

            this.createSimpleIntegerField(comp, "Frame Width", "frameConfig.frameWidth");

            this.createSimpleIntegerField(comp, "Frame Height", "frameConfig.frameHeight");

            this.createSimpleIntegerField(comp, "Start Frame", "frameConfig.startFrame");

            this.createSimpleIntegerField(comp, "End Frame", "frameConfig.endFrame");

            this.createSimpleIntegerField(comp, "Margin", "frameConfig.margin");

            this.createSimpleIntegerField(comp, "Spacing", "frameConfig.spacing");
        }
    }
}