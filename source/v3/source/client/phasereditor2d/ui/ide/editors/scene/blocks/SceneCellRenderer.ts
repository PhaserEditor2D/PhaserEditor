namespace phasereditor2d.ui.ide.editors.scene.blocks {

    const SceneThumbnailCache: Map<string, controls.IImage> = new Map();

    export class SceneCellRenderer implements controls.viewers.ICellRenderer {

        renderCell(args: controls.viewers.RenderCellArgs): void {

            const file = <core.io.FilePath>args.obj;

            const image = SceneThumbnailCache.get(file.getId());

            if (image) {

                image.paint(args.canvasContext, args.x, args.y, args.w, args.h, args.center);

            }

        }

        cellHeight(args: controls.viewers.RenderCellArgs): number {
            return args.viewer.getCellSize();
        }


        async preload(obj: any): Promise<controls.PreloadResult> {

            const file = <core.io.FilePath>obj;

            const id = file.getId();

            if (SceneThumbnailCache.has(id)) {

                const image = SceneThumbnailCache.get(id);

                return image.preload();

            }

            const thumbnail = new SceneThumbnail(file);

            SceneThumbnailCache.set(id, thumbnail);

            return await thumbnail.preload();

        }


    }

}