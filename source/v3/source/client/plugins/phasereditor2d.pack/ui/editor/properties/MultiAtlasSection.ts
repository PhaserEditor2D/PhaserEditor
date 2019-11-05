/// <reference path="./BaseSection.ts" />

namespace phasereditor2d.pack.ui.editor.properties {

    import controls = colibri.ui.controls;

    export class MultiatlasSection extends BaseSection {

        constructor(page : controls.properties.PropertyPage) {
            super(page, "phasereditor2d.pack.ui.editor.properties.MultiatlasSection", "Multiatlas");
        }

        canEdit(obj : any, n : number) {
            return super.canEdit(obj, n) && obj instanceof core.MultiatlasAssetPackItem;
        }

        protected createForm(parent: HTMLDivElement) {

            const comp = this.createGridElement(parent, 3);

            comp.style.gridTemplateColumns = "auto 1fr auto";

            this.createFileField(comp, "URL", "url", core.contentTypes.CONTENT_TYPE_MULTI_ATLAS);

            {
                // path

                this.createLabel(comp, "Path");

                const text = this.createText(comp, false);
                text.style.gridColumn = "2 / span 2";

                text.addEventListener("change", e => {
                    this.changeItemField("path", text.value, true);
                });

                this.addUpdater(() => {
                    const data = this.getSelection()[0].getData();
                    text.value = data.path;
                });
            }
        }
    }
}