
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

            menu.add(new controls.Action({
                text: "Rename",
                enabled: sel.length === 1,
                callback: () => this.onRenameFile()
            }));

            menu.add(new controls.Action({
                text: "Move",
                enabled: sel.length > 0
            }));

            menu.add(new controls.Action({
                text: "Delete",
                enabled: sel.length > 0,
                callback: () => {

                    const files = this._viewer.getSelection();

                    if (confirm(`Do you want to delete ${files.length} files?`)) {

                        if (files.length > 0) {
                            ide.FileUtils.deleteFiles_async(files);
                        }
                    }
                }
            }));
        }

        private onRenameFile() {

            const file: io.FilePath = this._viewer.getSelectionFirstElement();

            const parent = file.getParent();

            const dlg = new controls.dialogs.InputDialog();

            dlg.create();

            dlg.setTitle("Rename");

            dlg.setMessage("Enter the new name");

            dlg.setInitialValue(file.getName());

            dlg.setInputValidator(value => {

                if (value.indexOf("/") >= 0) {
                    return false;
                }

                if (parent) {

                    const file2 = parent.getFile(value) ?? null;

                    return file2 === null;
                }

                return false;
            });

            dlg.setResultCallback(result => {
                ide.FileUtils.renameFile_async(file, result);
            });

            dlg.validate();

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

                if (change.getAddedFiles().length === 1) {

                    const file = change.getAddedFiles()[0];

                    if (file.isFolder()) {

                        setTimeout(() => {

                            viewer.reveal(file);
                            viewer.setSelection([file]);

                        }, 100);
                    }
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