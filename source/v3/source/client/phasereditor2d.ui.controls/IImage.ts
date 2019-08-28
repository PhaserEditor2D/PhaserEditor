namespace phasereditor2d.ui.controls {

    export interface IImage {
        paint(context: CanvasRenderingContext2D, x: number, y: number, w: number, h: number): void;

        preload(): Promise<any>;
    }

}