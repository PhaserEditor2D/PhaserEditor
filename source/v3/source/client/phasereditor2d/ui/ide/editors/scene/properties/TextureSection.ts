namespace phasereditor2d.ui.ide.editors.scene.properties {

    export class TextureSection extends SceneSection<Phaser.GameObjects.Image> {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "SceneEditor.TextureSection", "Texture", true);
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
                const obj = this.getSelection()[0];
                const asset = obj.getEditorAsset();
                let img: controls.IImage;
                if (asset instanceof pack.AssetPackItem && asset.getType() === pack.IMAGE_TYPE) {
                    img = pack.AssetPackUtils.getImageFromPackUrl(asset.getData().url);
                } else if (asset instanceof pack.AssetPackImageFrame) {
                    img = asset;
                } else {
                    img = new controls.ImageWrapper(null);
                }

                imgControl.setImage(img);
                setTimeout(() => imgControl.resizeTo(), 1);
            });
        }

        canEdit(obj: any): boolean {
            return obj instanceof Phaser.GameObjects.Image;
        }

        canEditNumber(n: number): boolean {
            return n === 1;
        }


    }

}