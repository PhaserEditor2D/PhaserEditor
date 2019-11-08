namespace phasereditor2d.ide.ui.wizards {

    import controls = colibri.ui.controls;
    import viewers = colibri.ui.controls.viewers;
    import dialogs = colibri.ui.controls.dialogs;
    import io = colibri.core.io;

    export declare type CreateFileCallback = (folder: io.FilePath, filename: string) => void;

    export class NewFileDialog extends dialogs.Dialog {

        private _filteredViewer: controls.viewers.FilteredViewerInElement<controls.viewers.TreeViewer>;
        private _fileNameText: HTMLInputElement;
        private _createBtn: HTMLButtonElement;
        private _fileExtension: string;
        private _fileContent: string;
        private _fileCreatedCallback: (file: io.FilePath) => void;

        constructor() {
            super("NewFileDialog");

            this._fileExtension = "";
            this._fileContent = "";
        }

        protected createDialogArea() {

            const clientArea = document.createElement("div");
            clientArea.classList.add("DialogClientArea");

            clientArea.style.display = "grid";
            clientArea.style.gridTemplateRows = "1fr auto";
            clientArea.style.gridTemplateRows = "1fr";
            clientArea.style.gridRowGap = "5px";

            clientArea.appendChild(this.createCenterArea());

            clientArea.appendChild(this.createBottomArea());

            this.getElement().appendChild(clientArea);
        }

        private createBottomArea() {

            const bottomArea = document.createElement("div");
            bottomArea.classList.add("DialogSection");
            bottomArea.style.display = "grid";
            bottomArea.style.gridTemplateColumns = "auto 1fr";
            bottomArea.style.gridTemplateRows = "auto";
            bottomArea.style.columnGap = "10px";
            bottomArea.style.rowGap = "10px";
            bottomArea.style.alignItems = "center";

            {
                const label = document.createElement("label");
                label.innerText = "Location";
                bottomArea.appendChild(label);

                const text = document.createElement("input");
                text.type = "text";
                text.readOnly = true;
                bottomArea.appendChild(text);

                this._filteredViewer.getViewer().addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                    const file = this._filteredViewer.getViewer().getSelectionFirstElement() as io.FilePath;
                    text.value = file === null ? "" : `${file.getFullName()}/`;
                });
            }

            {
                const label = document.createElement("label");
                label.innerText = "Name";
                bottomArea.appendChild(label);

                const text = document.createElement("input");
                text.type = "text";
                bottomArea.appendChild(text);
                setTimeout(() => text.focus(), 10);
                text.addEventListener("keyup", e => this.validate());
                this._fileNameText = text;
            }

            return bottomArea;
        }

        private normalizedFileName() {
            let name = this._fileNameText.value;

            if (name.endsWith("." + this._fileExtension)) {
                return name;
            }

            return name + "." + this._fileExtension;
        }

        validate() {
            const folder = this._filteredViewer.getViewer().getSelectionFirstElement() as io.FilePath;

            let valid = folder !== null;

            if (valid) {

                const name = this.normalizedFileName();

                if (name.indexOf("/") >= 0 || name.trim() === "") {

                    valid = false;

                } else {

                    const file = folder.getFile(name);

                    if (file) {
                        valid = false;
                    }
                }
            }

            this._createBtn.disabled = !valid;
        }

        setFileCreatedCallback(callback: (file: io.FilePath) => void) {
            this._fileCreatedCallback = callback;
        }

        setFileContent(fileContent: string) {
            this._fileContent = fileContent;
        }

        setInitialFileName(filename: string) {
            this._fileNameText.value = filename;
        }

        setFileExtension(fileExtension: string) {
            this._fileExtension = fileExtension;
        }

        setInitialLocation(folder: io.FilePath) {
            this._filteredViewer.getViewer().setSelection([folder]);
            this._filteredViewer.getViewer().reveal(folder);
        }

        create() {

            super.create();

            this._createBtn = this.addButton("Create", () => this.createFile());

            this.addButton("Cancel", () => this.close());

            this.validate();
        }

        private async createFile() {

            const folder = this._filteredViewer.getViewer().getSelectionFirstElement() as io.FilePath;

            const name = this.normalizedFileName();

            const file = await colibri.ui.ide.FileUtils.createFile_async(folder, name, this._fileContent);

            this.close();

            if (this._fileCreatedCallback) {
                this._fileCreatedCallback(file);
            }
        }

        private createCenterArea() {

            const centerArea = document.createElement("div");

            this.createFilteredViewer();

            centerArea.appendChild(this._filteredViewer.getElement());

            return centerArea;
        }

        private createFilteredViewer() {

            const viewer = new viewers.TreeViewer();

            viewer.setLabelProvider(new files.ui.viewers.FileLabelProvider());
            viewer.setContentProvider(new files.ui.viewers.FileTreeContentProvider(true));
            viewer.setCellRendererProvider(new files.ui.viewers.FileCellRendererProvider());
            viewer.setInput(colibri.ui.ide.Workbench.getWorkbench().getProjectRoot());

            viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                this.validate();
            });

            this._filteredViewer = new viewers.FilteredViewerInElement(viewer);

            addEventListener(controls.EVENT_CONTROL_LAYOUT, (e: CustomEvent) => {
                this._filteredViewer.resizeTo();
            });
        }
    }
}