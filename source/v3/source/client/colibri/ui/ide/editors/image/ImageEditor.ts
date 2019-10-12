/// <reference path="../../FileEditor.ts" />

namespace colibri.ui.ide.editors.image {

    class ImageEditorFactory extends EditorFactory {

        constructor() {
            super("phasereditor2d.ImageEditorFactory");
        }

        acceptInput(input: any): boolean {
            if (input instanceof core.io.FilePath) {

                const file = <core.io.FilePath>input;
                const contentType = Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);
                if (contentType === CONTENT_TYPE_IMAGE) {
                    return true;
                }
            }
            return false;
        }

        createEditor(): EditorPart {
            return new ImageEditor();
        }

    }

    export class ImageEditor extends FileEditor {
        private _imageControl: controls.ImageControl;

        constructor() {
            super("phasereditor2d.ImageEditor");
            this.addClass("ImageEditor");
        }

        static getFactory(): EditorFactory {
            return new ImageEditorFactory();
        }

        async createPart() {

            this._imageControl = new controls.ImageControl();

            const container = document.createElement("div");
            container.classList.add("ImageEditorContainer");
            container.appendChild(this._imageControl.getElement());

            this.getElement().appendChild(container);

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

            this.dispatchTitleUpdatedEvent();
        }

        getIcon(): controls.IImage {
            const file = this.getInput();
            if (!file) {
                return super.getIcon();
            }
            
            const img = Workbench.getWorkbench().getFileImage(file);
            return img;
        }

        layout() {
            if (this._imageControl) {
                this._imageControl.resizeTo();
            }
        }

        setInput(input: core.io.FilePath) {
            super.setInput(input);

            if (this._imageControl) {
                this.updateImage();
            }
        }
    }

}