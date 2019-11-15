
namespace phasereditor2d.files.ui.views {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import io = colibri.core.io;

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

        fillContextMenu(menu: controls.Menu) {

            const sel = this._viewer.getSelection();

            menu.add(new actions.NewFileAction(this));

            menu.add(new actions.RenameFileAction(this));

            menu.add(new actions.MoveFilesAction(this));

            menu.add(new actions.DeleteFilesAction(this));
        }

        getPropertyProvider() {
            return this._propertyProvider;
        }

        protected createPart(): void {

            super.createPart();

            const wb = ide.Workbench.getWorkbench();

            const root = wb.getProjectRoot();

            const viewer = this._viewer;

            viewer.setLabelProvider(new viewers.FileLabelProvider());
            viewer.setContentProvider(new viewers.FileTreeContentProvider());
            viewer.setCellRendererProvider(new viewers.FileCellRendererProvider());
            viewer.setInput(root);

            viewer.repaint();

            viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, (e: CustomEvent) => {
                wb.openEditor(e.detail);
            });

            wb.getFileStorage().addChangeListener(change => {

                viewer.setInput(ide.FileUtils.getRoot());

                viewer.repaint();

                const files: io.FilePath[] = [];

                for (const filePath of change.getAddRecords()) {

                    const file = ide.FileUtils.getFileFromPath(filePath, true);

                    files.push(file);
                }

                if (files.length > 0) {

                    const firstFile = files[0];

                    setTimeout(() => {

                        viewer.reveal(firstFile);

                        viewer.setSelection(files);

                        viewer.repaint();

                    }, 100);
                }
            });

            wb.addEventListener(ide.EVENT_EDITOR_ACTIVATED, e => {

                const editor = wb.getActiveEditor();

                if (editor) {

                    const input = editor.getInput();

                    if (input instanceof io.FilePath) {

                        viewer.setSelection([input]);
                        viewer.reveal(input);
                    }
                }
            });
        }

        getIcon() {
            return controls.Controls.getIcon(ide.ICON_FOLDER);
        }
    }
}