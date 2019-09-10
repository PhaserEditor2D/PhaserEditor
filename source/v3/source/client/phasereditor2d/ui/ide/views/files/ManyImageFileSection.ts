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
            viewer.getCanvas().classList.add("PreviewBackground");

            const filteredViewer = new controls.viewers.FilteredViewer(viewer);
            filteredViewer.setHandlePosition(false);
            filteredViewer.style.position = "relative";
            parent.appendChild(filteredViewer.getElement());

            this.resizeTo(filteredViewer, parent);

            this.getPage().addEventListener(controls.CONTROL_LAYOUT_EVENT, (e: CustomEvent) => {
                this.resizeTo(filteredViewer, parent);
            })

            this.addUpdater(() => {
                viewer.setInput(this.getSelection());
                this.resizeTo(filteredViewer, parent);
            });
        }

        private resizeTo(filteredViewer: controls.viewers.FilteredViewer<controls.viewers.Viewer>, parent: HTMLElement) {
            setTimeout(() => {
                filteredViewer.setBounds({
                    width: parent.clientWidth,
                    height: parent.clientHeight
                });
                filteredViewer.getViewer().repaint();
            }, 10);
        }

        canEdit(obj: any): boolean {
            if (obj instanceof core.io.FilePath) {
                const ct = Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(obj);
                return ct === CONTENT_TYPE_IMAGE;
            }
            return false;
        }

        canEditNumber(n: number): boolean {
            return n > 1;
        }


    }
}