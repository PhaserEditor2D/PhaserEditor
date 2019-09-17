namespace phasereditor2d.ui.controls {
    
    export class ImageIcon implements IIcon {
        private _image: IImage;

        public constructor(image: IImage) {
            this._image = image;
        }

        paint(context: CanvasRenderingContext2D, x: number, y: number, w: number = 16, h: number = 16) {
            this._image.paint(context, x, y, w, h, true);
        }
    }
}