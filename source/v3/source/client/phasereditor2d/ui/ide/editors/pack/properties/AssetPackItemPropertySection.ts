/// <reference path="../../../../../../phasereditor2d.ui.controls/properties/PropertySection.ts" />

namespace phasereditor2d.ui.ide.editors.pack.properties {

    export class AssetPackItemPropertySection extends controls.properties.PropertySection<AssetPackItem> {

        constructor(page : controls.properties.PropertyPage) {
            super(page, "AssetPackItemPropertySection", "File Key", false)
        }

        protected createForm(parent: HTMLDivElement) {
            const comp = this.createGridElement(parent, 2);

            {
                // Key

                this.createLabel(comp, "Key");
                const text = this.createText(comp, true);
                this.addUpdater(() => {
                    text.value = this.flatValues_String(this.getSelection().map(item => item.getKey()));
                });
            }
        }

        canEdit(obj: any): boolean {
            return obj instanceof AssetPackItem;
        }

        canEditNumber(n: number): boolean {
            return n === 1;
        }

    }

}