namespace phasereditor2d.files.ui.views {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import io = colibri.core.io;

    export class UploadSection extends controls.properties.PropertySection<io.FilePath> {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "phasereditor2d.files.ui.views", "Upload", true);
        }

        protected createForm(parent: HTMLDivElement) {

            const comp = this.createGridElement(parent, 1);
            comp.classList.add("UploadSection");
            comp.style.display = "grid";
            comp.style.gridTemplateColumns = "1fr";
            comp.style.gridTemplateRows = "auto 1fr auto";
            comp.style.gridGap = "5px";

            const filesInput = document.createElement("input");

            const uploadBtn = document.createElement("button");

            const filesViewer = new controls.viewers.TreeViewer();
            const filesFilteredViewer = new ide.properties.FilteredViewerInPropertySection(this.getPage(), filesViewer);

            {
                // browse button

                const browseBtn = document.createElement("button");
                browseBtn.innerText = "Browse";
                browseBtn.style.alignItems = "start";
                browseBtn.addEventListener("click", e => filesInput.click());
                comp.appendChild(browseBtn);
            }

            {
                // file list

                filesViewer.setLabelProvider(new viewers.InputFileLabelProvider());
                filesViewer.setCellRendererProvider(new viewers.InputFileCellRendererProvider());
                filesViewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
                filesViewer.setInput([]);

                comp.appendChild(filesFilteredViewer.getElement());

                this.addUpdater(() => {

                    filesViewer.setInput([]);
                    filesViewer.repaint();
                });
            }

            {

                filesInput.type = "file";
                filesInput.name = "files";
                filesInput.multiple = true;
                filesInput.addEventListener("change", e => {

                    const files = filesInput.files;

                    const input = [];

                    for (let i = 0; i < files.length; i++) {
                        input.push(files.item(i));
                    }

                    filesViewer.setInput(input);

                    filesViewer.repaint();

                    uploadBtn.disabled = input.length === 0;
                });

                comp.appendChild(filesInput);

                this.addUpdater(() => {
                    filesInput.value = "";
                });

                {

                    // submit button

                    uploadBtn.disabled = true;
                    uploadBtn.innerText = "Upload";
                    uploadBtn.addEventListener("click", async (e) => {

                        const input = filesViewer.getInput() as File[];

                        const files = input.slice();

                        const uploadFolder = this.getSelection()[0] as io.FilePath;

                        const cancelFlag = {
                            canceled: false
                        };

                        const dlg = new controls.dialogs.ProgressDialog();
                        dlg.create();
                        dlg.setTitle("Uploading");
                        dlg.setCloseWithEscapeKey(false);

                        {
                            const btn = dlg.addButton("Cancel", () => {

                                if (cancelFlag.canceled) {
                                    return;
                                }

                                cancelFlag.canceled = true;

                                btn.innerText = "Canceling";
                            });
                        }

                        dlg.setProgress(0);

                        for (const file of files) {

                            if (cancelFlag.canceled) {
                                dlg.close();
                                break;
                            }

                            const formData = new FormData();
                            formData.append("files", file);
                            formData.append("uploadTo", uploadFolder.getFullName());

                            const resp = await fetch("upload", {
                                method: "POST",
                                body: formData
                            });

                            const respData = await resp.json();

                            if (respData.error) {

                                alert(`Error sending file ${file.name}`);
                                break;
                            }

                            input.shift();

                            filesViewer.repaint();

                            dlg.setProgress(1 - (input.length / files.length));
                        }

                        dlg.close();
                        
                        uploadBtn.disabled = (filesViewer.getInput() as File[]).length === 0;
                    });

                    comp.appendChild(uploadBtn);
                }
            }
        }

        canEdit(obj: any, n: number): boolean {
            return obj instanceof io.FilePath && obj.isFolder();
        }

        canEditNumber(n: number): boolean {
            return n === 1;
        }
    }
}