/// <reference path="./BaseSection.ts" />

namespace phasereditor2d.pack.ui.editor.properties {

    import controls = colibri.ui.controls;

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


            for (const info of [
                ["Frame Width", "frameWidth"],
                ["Frame Height", "frameHeight"],
                ["Start Frame", "startFrame"],
                ["End Frame", "endFrame"],
                ["Margin", "margin"],
                ["Spacing", "spacing"]
            ]) {
                const label = info[0];
                const field = `frameConfig.${info[1]}`;

                this.createLabel(comp, label);

                const text = this.createText(comp, false);
                text.style.gridColumn = "2 / span 2";

                text.addEventListener("change", e => {
                    this.changeItemField(field, Number.parseInt(text.value), true);
                });

                this.addUpdater(() => {
                    const data = this.getSelection()[0].getData();
                    text.value = undo.ChangeItemFieldOperation.getDataValue(data, field);
                });
            }

        }
    }
}