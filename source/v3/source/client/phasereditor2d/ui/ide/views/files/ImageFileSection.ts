namespace phasereditor2d.ui.ide.files {
    export class ImageFileSection extends controls.properties.PropertySection<core.io.FilePath> {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "files.ImageFileSection", "Image File", true);
        }

        protected createForm(parent: HTMLDivElement) {
            
            //var comp = this.createGridElement(parent, 1);

            const wrapper = new controls.ImageWrapper();
            const canvas = wrapper.getCanvas();

            this.getPage().addEventListener(controls.CONTROL_LAYOUT_EVENT, (e : CustomEvent)  => {
                console.log("repaint on layout!!!");
                wrapper.repaint();
            })

            canvas.style.width = "100%";
            canvas.style.height = "95%";
            canvas.style.alignSelf = "center";

            parent.appendChild(canvas);

            this.addUpdater(() => {
                const file = this.getSelection()[0];
                const img = Workbench.getWorkbench().getFileImage(file);
                wrapper.setImage(img);
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