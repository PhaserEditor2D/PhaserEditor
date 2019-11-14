
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
                text: "New...",
                enabled: true,
                callback: () => this.onNewFile()
            }));

            menu.add(new controls.Action({
                text: "Rename",
                enabled: sel.length === 1,
                callback: () => this.onRenameFile()
            }));

            menu.add(new controls.Action({
                text: "Move",
                enabled: sel.length > 0,
                callback: () => this.onMoveFiles()
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

        private onMoveFiles() {

            const rootFolder = colibri.ui.ide.FileUtils.getRoot();

            const viewer = new controls.viewers.TreeViewer();

            viewer.setLabelProvider(new viewers.FileLabelProvider());
            viewer.setCellRendererProvider(new viewers.FileCellRendererProvider());
            viewer.setContentProvider(new viewers.FileTreeContentProvider(true));
            viewer.setInput(rootFolder);
            viewer.setExpanded(rootFolder, true);

            const dlg = new controls.dialogs.ViewerDialog(viewer);

            dlg.create();

            dlg.setTitle("Move Files");

            {
                const btn = dlg.addButton("Move", () => { });

                btn.disabled = true;

                viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {

                    const sel = viewer.getSelection();

                    let enabled = true;

                    if (sel.length !== 1) {

                        enabled = false;

                    } else {

                        const moveTo = sel[0] as io.FilePath;

                        for (const obj of this.getViewer().getSelection()) {

                            const file = obj as io.FilePath;

                            if (
                                moveTo.getFullName().startsWith(file.getFullName())
                                || moveTo === file.getParent()
                                || moveTo.getFile(file.getName())
                            ) {
                                enabled = false;
                                break;
                            }
                        }
                    }

                    btn.disabled = !enabled;
                });

                btn.addEventListener("click", () => {
                    
                    const moveTo = viewer.getSelectionFirstElement() as io.FilePath;
                    
                    const movingFiles = this.getViewer().getSelection();
                    
                    colibri.ui.ide.FileUtils.moveFiles_async(movingFiles, moveTo);

                    dlg.close();
                });
            }

            dlg.addButton("Cancel", () => dlg.close());
        }

        private onNewFile() {

            const action = new actions.OpenNewFileDialogAction();

            let folder = this._viewer.getSelectionFirstElement() as io.FilePath;

            if (folder) {

                if (folder.isFile()) {
                    folder = folder.getParent();
                }

                action.setInitialLocation(folder);
            }


            action.run();
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

                const files : io.FilePath[] = [];

                for(const filePath of change.getAddRecords()) {

                    const file = ide.FileUtils.getFileFromPath(filePath, true);

                    files.push(file);
                }

                if (files.length === 1) {

                    const file = files[0];

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