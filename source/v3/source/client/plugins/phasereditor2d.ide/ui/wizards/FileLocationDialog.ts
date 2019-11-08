namespace phasereditor2d.ide.ui.wizards {

    import controls = colibri.ui.controls;
    import viewers = colibri.ui.controls.viewers;
    import dialogs = colibri.ui.controls.dialogs;
    import io = colibri.core.io;

    export class FileLocationDialog extends dialogs.Dialog {

        private _filteredViewer: controls.viewers.FilteredViewerInElement<controls.viewers.TreeViewer>;
        private _fileNameText: HTMLInputElement;
        private _createBtn: HTMLButtonElement;

        constructor() {
            super("FileLocationDialog");
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

        private validate() {
            const folder = this._filteredViewer.getViewer().getSelectionFirstElement() as io.FilePath;

            let valid = folder !== null;

            if (valid) {

                const name = this._fileNameText.value;

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

        setInitialFileName(filename: string) {
            this._fileNameText.value = filename;
        }

        create() {

            super.create();

            this._createBtn = this.addButton("Create", () => { });

            this.addButton("Cancel", () => this.close());

            this.validate();
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