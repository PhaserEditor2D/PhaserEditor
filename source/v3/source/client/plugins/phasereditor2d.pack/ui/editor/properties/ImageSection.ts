namespace phasereditor2d.pack.ui.editor.properties {

    import controls = colibri.ui.controls;

    export class ImageSection extends BaseSection {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "phasereditor2d.pack.ui.editor.properties.ImageSection", "Image");
        }

        protected createForm(parent: HTMLDivElement) {
            const comp = this.createGridElement(parent, 3);

            comp.style.gridTemplateColumns = "auto 1fr auto";

            {
                // url

                this.createLabel(comp, "URL");

                const text = this.createText(comp, true);

                this.addUpdater(() => {
                    text.value = this.getSelection()[0].getData().url;
                });

                this.createButton(comp, "Browse", () => {

                    this.browseFile_onlyContentType("Select Image", files.core.CONTENT_TYPE_IMAGE, (files) => {

                    });

                });
            }

        }
    }
}