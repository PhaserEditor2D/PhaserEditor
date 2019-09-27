/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/TreeViewer.ts" />
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/GridTreeViewerRenderer.ts" />

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

            const filteredViewer = new controls.viewers.FilteredViewer(viewer);
            filteredViewer.setHandlePosition(false);
            filteredViewer.style.position = "relative";
            filteredViewer.style.height = "100%";
            parent.appendChild(filteredViewer.getElement());

            this.resizeTo(filteredViewer, parent);

            this.getPage().addEventListener(controls.EVENT_CONTROL_LAYOUT, (e: CustomEvent) => {
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