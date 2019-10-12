
namespace phasereditor2d.ui.ide.views.files {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;
    import viewers = colibri.ui.controls.viewers;

    export class FilesView extends ide.ViewerView {

        private _propertyProvider = new FilePropertySectionProvider();

        constructor() {
            super("filesView");
            this.setTitle("Files");
            this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FOLDER));
        }

        protected createViewer() {
            return new viewers.TreeViewer();
        }

        getPropertyProvider() {
            return this._propertyProvider;
        }

        protected createPart(): void {
            super.createPart();

            const root = ide.Workbench.getWorkbench().getProjectRoot();

            const viewer = this._viewer;
            viewer.setLabelProvider(new FileLabelProvider());
            viewer.setContentProvider(new FileTreeContentProvider());
            viewer.setCellRendererProvider(new FileCellRendererProvider());
            viewer.setInput(root);

            viewer.repaint();

            viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, (e: CustomEvent) => {
                ide.Workbench.getWorkbench().openEditor(e.detail);
            });
        }

        getIcon() {
            return controls.Controls.getIcon(ide.ICON_FOLDER);
        }

    }
}