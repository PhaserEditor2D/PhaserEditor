namespace phasereditor2d.ui.ide.editors.scene.blocks {
    
    import controls = colibri.ui.controls;
    import core = colibri.core;

    export class SceneCellRenderer implements controls.viewers.ICellRenderer {

        renderCell(args: controls.viewers.RenderCellArgs): void {

            const file = <core.io.FilePath>args.obj;

            const image = SceneThumbnailCache.getInstance().getContent(file);

            image.paint(args.canvasContext, args.x, args.y, args.w, args.h, args.center);
        }

        cellHeight(args: controls.viewers.RenderCellArgs): number {
            return args.viewer.getCellSize();
        }


        async preload(obj: any): Promise<controls.PreloadResult> {
            
            const file = <core.io.FilePath>obj;

            return SceneThumbnailCache.getInstance().preload(file);
        }
    }

}