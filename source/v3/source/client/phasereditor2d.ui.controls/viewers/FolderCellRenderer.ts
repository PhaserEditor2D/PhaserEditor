namespace phasereditor2d.ui.controls.viewers {
    export class FolderCellRenderer implements ICellRenderer {

        private _maxCount: number;

        constructor(maxCount: number = 8) {
            this._maxCount = maxCount;
        }

        renderCell(args: RenderCellArgs): void {
            if (this.cellHeight(args) === ROW_HEIGHT) {
                this.renderFolder(args);
            } else {
                this.renderGrid(args);
            }
        }

        private renderFolder(args: RenderCellArgs) {
            const icon = ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FOLDER);
            icon.paint(args.canvasContext, args.x, args.y, args.w, args.h, true);
        }

        protected renderGrid(args: RenderCellArgs) {
            const contentProvider = <ITreeContentProvider>args.viewer.getContentProvider();
            const children = contentProvider.getChildren(args.obj);

            const width = args.w - 20;
            const height = args.h - 2;

            if (children) {

                const realCount = children.length;
                let frameCount = realCount;
                if (frameCount == 0) {
                    return;
                }

                let step = 1;

                if (frameCount > this._maxCount) {
                    step = frameCount / this._maxCount;
                    frameCount = this._maxCount;
                }

                var size = Math.floor(Math.sqrt(width * height / frameCount) * 0.8) + 1;

                var cols = width / size;
                var rows = frameCount / cols + (frameCount % cols == 0 ? 0 : 1);
                var marginX = Math.max(0, (width - cols * size) / 2);
                var marginY = Math.max(0, (height - rows * size) / 2);

                var itemX = 0;
                var itemY = 0;

                const startX = 20 + args.x + marginX;
                const startY = 2 + args.y + marginY;


                for (var i = 0; i < frameCount; i++) {
                    if (itemY + size > height) {
                        break;
                    }

                    const index = Math.min(realCount - 1, Math.round(i * step));
                    const obj = children[index];
                    const renderer = args.viewer.getCellRendererProvider().getCellRenderer(obj);

                    const args2 = new RenderCellArgs(args.canvasContext,
                        startX + itemX, startY + itemY,
                        size, size,
                        obj, args.viewer, true
                    );

                    renderer.renderCell(args2);

                    itemX += size;

                    if (itemX + size > width) {
                        itemY += size;
                        itemX = 0;
                    }
                }

            }
        }

        cellHeight(args: RenderCellArgs): number {
            return args.viewer.getCellSize() < 50 ? ROW_HEIGHT : args.viewer.getCellSize();
        }

        preload(obj: any): Promise<PreloadResult> {
            return Controls.resolveNothingLoaded();
        }

    }
}