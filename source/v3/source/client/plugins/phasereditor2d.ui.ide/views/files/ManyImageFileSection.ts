
namespace phasereditor2d.ui.ide.views.files {

    class GridImageFileViewer extends controls.viewers.TreeViewer {

        constructor(...classList: string[]) {
            super("PreviewBackground", ...classList);

            this.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
            this.setLabelProvider(new files.FileLabelProvider());
            this.setCellRendererProvider(new files.FileCellRendererProvider());
            this.setTreeRenderer(new controls.viewers.GridTreeViewerRenderer(this, true));

            this.getCanvas().classList.add("PreviewBackground");
        }
    }

    export class ManyImageFileSection extends controls.properties.PropertySection<core.io.FilePath> {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "files.ManyImageFileSection", "Images", true);
        }

        protected createForm(parent: HTMLDivElement) {
            parent.classList.add("ManyImagePreviewFormArea");

            const viewer = new GridImageFileViewer();

            const filteredViewer = new properties.FilteredViewerInPropertySection(this.getPage(), viewer);
            parent.appendChild(filteredViewer.getElement());

            this.addUpdater(() => {
                
                // clean the viewer first
                viewer.setInput([]);
                viewer.repaint();

                viewer.setInput(this.getSelection());

                filteredViewer.resizeTo();
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
            return n > 1;
        }


    }
}