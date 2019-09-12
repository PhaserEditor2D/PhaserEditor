namespace phasereditor2d.ui.controls.viewers {

  export class RenderCellArgs {
    constructor(
      public canvasContext: CanvasRenderingContext2D,
      public x: number,
      public y: number,
      public w: number,
      public h: number,
      public obj: any,
      public viewer: Viewer,
      public center = false) {
    }
  };

}