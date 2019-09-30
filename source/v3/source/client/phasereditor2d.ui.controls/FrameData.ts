namespace phasereditor2d.ui.controls {
   
    export class FrameData {
        constructor(
            public index: number,
            public src: controls.Rect,
            public dst: controls.Rect,
            public srcSize: controls.Point,
        ) {

        }
    }
}