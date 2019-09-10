namespace phasereditor2d.ui.ide.files {

    export class ImageFileSection extends controls.properties.PropertySection<core.io.FilePath> {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "files.ImagePreviewSection", "Image", true);
        }

        protected createForm(parent: HTMLDivElement) {
 
            parent.classList.add("ImagePreviewFormArea");

            const imgControl = new controls.ImageControl(IMG_SECTION_PADDING);

            this.getPage().addEventListener(controls.CONTROL_LAYOUT_EVENT, (e: CustomEvent) => {
                imgControl.resizeTo(parent);
            })

            parent.appendChild(imgControl.getElement());
            setTimeout(() => imgControl.resizeTo(), 1);

            this.addUpdater(() => {
                const file = this.getSelection()[0];
                const img = Workbench.getWorkbench().getFileImage(file);
                imgControl.setImage(img);
                //imgControl.resizeTo(parent);
                setTimeout(() => imgControl.resizeTo(), 1);
            });
        }

        canEdit(obj: any): boolean {
            if (obj instanceof core.io.FilePath) {
                const ct = Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(obj);
                return ct === CONTENT_TYPE_IMAGE;
            }
            return false;
        }

        canEditNumber(n: number): boolean {
            return n == 1;
        }

    }
}