/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySectionProvider.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySection.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>
/// <reference path="../../ViewerView.ts"/>

namespace phasereditor2d.ui.ide.views.files {

    import viewers = phasereditor2d.ui.controls.viewers;

    export class FilesView extends ide.ViewerView {

        private _propertyProvider = new FilePropertySectionProvider();

        constructor() {
            super("filesView");
            this.setTitle("Files");
            this.setIcon(Workbench.getWorkbench().getWorkbenchIcon(ICON_FOLDER));
        }

        protected createViewer() {
            return new viewers.TreeViewer();
        }

        getPropertyProvider() {
            return this._propertyProvider;
        }

        protected createPart(): void {
            super.createPart();

            const root = Workbench.getWorkbench().getProjectRoot();

            const viewer = this._viewer;
            viewer.setLabelProvider(new FileLabelProvider());
            viewer.setContentProvider(new FileTreeContentProvider());
            viewer.setCellRendererProvider(new FileCellRendererProvider());
            viewer.setInput(root);

            viewer.repaint();

            viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, (e: CustomEvent) => {
                Workbench.getWorkbench().openEditor(e.detail);
            });
        }

        getIcon() {
            return controls.Controls.getIcon(ICON_FOLDER);
        }

    }
}