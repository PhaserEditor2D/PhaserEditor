
namespace phasereditor2d.files.ui.views {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export class FilesView extends ide.ViewerView {

        private _propertyProvider = new FilePropertySectionProvider();

        constructor() {
            super("filesView");
            this.setTitle("Files");
            this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FOLDER));
        }

        protected createViewer() {
            return new controls.viewers.TreeViewer();
        }

        getPropertyProvider() {
            return this._propertyProvider;
        }

        protected createPart(): void {
            super.createPart();

            const root = ide.Workbench.getWorkbench().getProjectRoot();

            const viewer = this._viewer;
            viewer.setLabelProvider(new viewers.FileLabelProvider());
            viewer.setContentProvider(new viewers.FileTreeContentProvider());
            viewer.setCellRendererProvider(new viewers.FileCellRendererProvider());
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