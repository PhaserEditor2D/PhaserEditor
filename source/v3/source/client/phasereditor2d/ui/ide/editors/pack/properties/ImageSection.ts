namespace phasereditor2d.ui.ide.editors.pack.properties {

    export class ImageSection extends controls.properties.PropertySection<AssetPackItem> {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "id", "Image Key Preview", true);
        }

        protected createForm(parent: HTMLDivElement) {
            parent.classList.add("ImagePreviewFormArea", "PreviewBackground");

            const imgControl = new controls.ImageControl(IMG_SECTION_PADDING);

            this.getPage().addEventListener(controls.EVENT_CONTROL_LAYOUT, (e: CustomEvent) => {
                imgControl.resizeTo();
            })

            parent.appendChild(imgControl.getElement());
            setTimeout(() => imgControl.resizeTo(), 1);

            this.addUpdater(() => {
                const packItem = this.getSelection()[0];
                const img = AssetPackUtils.getImageFromPackUrl(packItem.getData().url);
                imgControl.setImage(img);
                setTimeout(() => imgControl.resizeTo(), 1);
            });
        }

        canEdit(obj: any): boolean {
            return obj instanceof AssetPackItem && obj.getType() === "image";
        }

        canEditNumber(n: number): boolean {
            return n === 1;
        }


    }

}