/// <reference path="./BaseSection.ts" />

namespace phasereditor2d.pack.ui.editor.properties {

    import controls = colibri.ui.controls;

    export class SVGSection extends BaseSection {

        constructor(page : controls.properties.PropertyPage) {
            super(page, "phasereditor2d.pack.ui.editor.properties.SVGSection", "SVG");
        }

        canEdit(obj : any, n : number) {
            return super.canEdit(obj, n) && obj instanceof core.SvgAssetPackItem;
        }

        protected createForm(parent: HTMLDivElement) {

            const comp = this.createGridElement(parent, 3);

            comp.style.gridTemplateColumns = "auto 1fr auto";

            {
                // URL
                this.createFileField(comp, "URL", "url", files.core.CONTENT_TYPE_SVG);
            }

            {
                // svgConfig.width

                this.createLabel(comp, "Width");

                const text = this.createText(comp, false);
                text.style.gridColumn = "2 / span 2";

                text.addEventListener("change", e => {
                    this.changeItemField("svgConfig.width", Number.parseInt(text.value), true);
                });

                this.addUpdater(() => {
                    const data = this.getSelection()[0].getData();
                    text.value = data.svgConfig.width.toString();
                });
            }

            {
                // svgConfig.height

                this.createLabel(comp, "Height");

                const text = this.createText(comp, false);
                text.style.gridColumn = "2 / span 2";

                text.addEventListener("change", e => {
                    this.changeItemField("svgConfig.height", Number.parseInt(text.value), true);
                });

                this.addUpdater(() => {
                    const data = this.getSelection()[0].getData();
                    text.value = data.svgConfig.height.toString();
                });
            }

        }

    }
}