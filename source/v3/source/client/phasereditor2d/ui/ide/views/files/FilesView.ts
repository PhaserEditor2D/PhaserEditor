/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySectionProvider.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySection.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.ide.files {

    import viewers = phasereditor2d.ui.controls.viewers;

    export class FilesView extends ide.ViewPart {
        constructor() {
            super("filesView");
            this.setTitle("Files");

            //const root = new core.io.FilePath(null, TEST_DATA);

            const root = Workbench.getWorkbench().getFileStorage().getRoot();

            //console.log(root.toStringTree());

            const viewer = new viewers.TreeViewer();
            viewer.setLabelProvider(new FileLabelProvider());
            viewer.setContentProvider(new FileTreeContentProvider());
            viewer.setCellRendererProvider(new FileCellRendererProvider());
            // viewer.setTreeRenderer(new viewers.GridTreeRenderer(viewer));
            viewer.setInput(root);

            const filteredViewer = new viewers.FilteredViewer(viewer);

            this.getClientArea().add(filteredViewer);
            this.getClientArea().setLayout(new ui.controls.FillLayout());

            viewer.repaint();

            viewer.addEventListener(controls.SELECTION_EVENT, (e: CustomEvent) => {
                this.setSelection(e.detail);
            });
        }

        private _propertyProvider = new FilePropertySectionProvider();

        getPropertyProvider() {
            return this._propertyProvider;
        }

    }
}