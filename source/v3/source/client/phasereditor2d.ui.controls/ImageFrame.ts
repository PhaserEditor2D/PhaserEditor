namespace phasereditor2d.ui.controls {

    export class ImageFrame {
        private _name: string;
        private _image: controls.IImage;
        private _frameData: FrameData;

        constructor(name: string, image: controls.IImage, frameData: FrameData) {
            this._name = name;
            this._image = image;
            this._frameData = frameData;
        }

        getName() {
            return this._name;
        }

        getImage() {
            return this._image;
        }

        getFrameData() {
            return this._frameData;
        }
    }
}