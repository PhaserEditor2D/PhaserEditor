namespace phasereditor2d.ui.controls.viewers {

    export class PaintItem extends controls.Rect {
        constructor(
            public index: number,
            public data: any
        ) {
            super();
        }
    }
}