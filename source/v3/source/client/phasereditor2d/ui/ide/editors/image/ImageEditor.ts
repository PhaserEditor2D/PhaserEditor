namespace phasereditor2d.ui.ide.editors.image {

    class ImageEditorFactory extends EditorFactory {

        public constructor() {
            super("phasereditor2d.ImageEditorFactory");
        }

        public acceptInput(input: any): boolean {
            if (input instanceof core.io.FilePath) {

                const file = <core.io.FilePath>input;
                const contentType = Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);
                if (contentType === CONTENT_TYPE_IMAGE) {
                    return true;
                }
            }
            return false;
        }

        public createEditor(): EditorPart {
            return new ImageEditor();
        }


    }

    export class ImageEditor extends EditorPart {
        private _imageControl: controls.ImageControl;

        constructor() {
            super("phasereditor2d.ImageEditor");
        }

        public static getFactory(): EditorFactory {
            return new ImageEditorFactory();
        }

        async createPart() {
            super.createPart();

            this._imageControl = new controls.ImageControl();

            this.add(this._imageControl);

            this.updateImage();
        }

        private async updateImage() {
            const file = this.getInput();

            if (!file) {
                return;
            }

            const img = Workbench.getWorkbench().getFileImage(file);

            this._imageControl.setImage(img);

            this._imageControl.repaint();

            const result = await img.preload();

            if (result === controls.PreloadResult.RESOURCES_LOADED) {
                this._imageControl.repaint();
            }
        }

        layout() {
            if (this._imageControl) {
                this._imageControl.resizeTo();
            }
        }

        public setInput(input: core.io.FilePath) {
            super.setInput(input);
            this.setTitle(input.getName());
            if (this._imageControl) {
                this.updateImage();
            }
        }
    }

}