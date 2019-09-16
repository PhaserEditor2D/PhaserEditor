namespace phasereditor2d.ui.ide.views.files {

    export class FileSection extends controls.properties.PropertySection<core.io.FilePath> {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "files.FileSection", "File");
        }

        protected createForm(parent: HTMLDivElement) {
            const comp = this.createGridElement(parent, 2);
            {
                // Name

                this.createLabel(comp, "Name");
                const text = this.createText(comp, true);
                this.addUpdater(() => {
                    text.value = this.flatValues_String(this.getSelection().map(file => file.getName()));
                });
            }

            {
                // Full Name

                this.createLabel(comp, "Full Name");
                const text = this.createText(comp, true);
                this.addUpdater(() => {
                    text.value = this.flatValues_String(this.getSelection().map(file => file.getFullName()));
                });
            }

            {
                // Size

                this.createLabel(comp, "Size");
                const text = this.createText(comp, true);
                this.addUpdater(() => {
                    text.value = this.getSelection()
                        .map(f => f.getSize())
                        .reduce((a, b) => a + b)
                        .toString();
                })
            }

        }

        canEdit(obj: any): boolean {
            return obj instanceof core.io.FilePath;
        }

        canEditNumber(n: number): boolean {
            return n > 0;
        }


    }
}