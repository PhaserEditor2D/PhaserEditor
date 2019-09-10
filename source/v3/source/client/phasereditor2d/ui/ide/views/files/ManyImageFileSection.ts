namespace phasereditor2d.ui.ide.files {
    export class ManyImageFileSection extends controls.properties.PropertySection<core.io.FilePath> {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "files.ManyImageFileSection", "Images", true);
        }

        protected createForm(parent: HTMLDivElement) {

            parent.classList.add("ManyImagePreviewFormArea");

            const viewer = new ui.controls.viewers.GridViewer();
            viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
            viewer.setLabelProvider(new files.FileLabelProvider());
            viewer.setCellRendererProvider(new files.FileCellRendererProvider());

            this.getPage().addEventListener(controls.CONTROL_LAYOUT_EVENT, (e: CustomEvent) => {
                console.log("resize");
                this.resizeTo(viewer, parent);
            })

            parent.appendChild(viewer.getElement());
            setTimeout(() => this.resizeTo(viewer, parent), 1);

            this.addUpdater(() => {
                viewer.setInput(this.getSelection());
                this.resizeTo(viewer, parent);
            });

        }

        private resizeTo(viewer: controls.viewers.GridViewer, parent: HTMLElement) {
            viewer.style.width = parent.clientWidth + "px";
            viewer.style.height = parent.clientHeight + "px";
            viewer.repaint();
        }

        canEdit(obj: any): boolean {
            return obj instanceof core.io.FilePath;
        }

        canEditNumber(n: number): boolean {
            return n > 1;
        }


    }
}