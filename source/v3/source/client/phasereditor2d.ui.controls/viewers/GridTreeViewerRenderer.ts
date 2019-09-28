/// <reference path="./TreeViewerRenderer.ts" />

namespace phasereditor2d.ui.controls.viewers {

    const GRID_PADDING = 5;

    export class GridTreeViewerRenderer extends TreeViewerRenderer {

        private _center: boolean;

        constructor(viewer: TreeViewer, center: boolean = false) {
            super(viewer);
            viewer.setCellSize(128);
            this._center = center;
        }

        protected paintItems(objects: any[], treeIconList: TreeIconInfo[], paintItems: PaintItem[], x: number, y: number) {
            const viewer = this.getViewer();

            if (viewer.getCellSize() <= 48) {
                return super.paintItems(objects, treeIconList, paintItems, x, y);
            }

            const b = viewer.getBounds();

            const offset = this._center ? Math.floor(b.width % (viewer.getCellSize() + GRID_PADDING) / 2) : 0;

            return this.paintItems2(objects, treeIconList, paintItems, x + offset, y + GRID_PADDING, offset, 0);
        }

        private paintItems2(objects: any[], treeIconList: TreeIconInfo[], paintItems: PaintItem[], x: number, y: number, offset: number, extra: number) {

            const viewer = this.getViewer();
            const cellSize = Math.max(ROW_HEIGHT, viewer.getCellSize());
            const context = viewer.getContext();

            const b = viewer.getBounds();

            for (let obj of objects) {

                const children = viewer.getContentProvider().getChildren(obj);
                const expanded = viewer.isExpanded(obj);

                if (viewer.isFilterIncluded(obj)) {

                    const renderer = viewer.getCellRendererProvider().getCellRenderer(obj);

                    const args = new RenderCellArgs(context, x, y, cellSize, cellSize, obj, viewer, true);

                    let cellExtra = this.renderGridCell(args, renderer);
                    extra = Math.max(cellExtra, extra);

                    if (y > -cellSize && y < b.height) {
                        // render tree icon
                        if (children.length > 0) {
                            const iconY = y + (cellSize - TREE_ICON_SIZE) / 2;

                            const icon = Controls.getIcon(expanded ? ICON_CONTROL_TREE_COLLAPSE : ICON_CONTROL_TREE_EXPAND);
                            icon.paint(context, x + 5, iconY, ICON_SIZE, ICON_SIZE, false);

                            treeIconList.push({
                                rect: new Rect(x, iconY, TREE_ICON_SIZE, TREE_ICON_SIZE),
                                obj: obj
                            });
                        }
                    }

                    const item = new PaintItem(paintItems.length, obj);
                    item.set(args.x, args.y, args.w, args.h + cellExtra);
                    paintItems.push(item);

                    x += cellSize + GRID_PADDING;

                    if (x + cellSize > b.width) {
                        y += cellSize + extra + GRID_PADDING;
                        x = 0 + offset;
                        extra = 0;
                    }
                }

                if (expanded) {
                    const result = this.paintItems2(children, treeIconList, paintItems, x, y, offset, extra);
                    y = result.y;
                    x = result.x;
                    extra = Math.max(extra, result.extra);
                }
            }

            return {
                x: x,
                y: y,
                extra: extra
            };
        }

        private renderGridCell(args: RenderCellArgs, renderer: ICellRenderer): number {
            const cellSize = args.viewer.getCellSize();
            const b = args.viewer.getBounds();
            const lineHeight = 20;
            let x = args.x;
            let y = args.y;

            const ctx = args.canvasContext;

            const label = args.viewer.getLabelProvider().getLabel(args.obj);
            let lines = [""];
            for (const c of label) {
                const test = lines[lines.length - 1] + c;
                const m = ctx.measureText(test);
                if (m.width > args.w) {
                    if (lines.length === 2) {
                        let str = lines[lines.length - 1];
                        if (str.length > 2) {
                            str = str.substring(0, str.length - 2) + "..";
                        }
                        lines[lines.length - 1] = str;
                        break;
                    } else {
                        lines.push("");
                        lines[lines.length - 1] = c;
                    }
                } else {
                    lines[lines.length - 1] += c;
                }
            }

            const selected = args.viewer.isSelected(args.obj);

            let strH: number;
            let visible: boolean;

            {
                const args2 = new RenderCellArgs(
                    args.canvasContext,
                    args.x + 3,
                    args.y + 3,
                    args.w - 6,
                    args.h - 6,
                    args.obj,
                    args.viewer,
                    true
                );

                strH = lines.length * lineHeight;

                visible = args.y > -(cellSize + strH) && args.y < b.height;

                if (visible) {
                    if (selected) {
                        ctx.save();

                        ctx.fillStyle = Controls.theme.treeItemSelectionBackground;

                        ctx.globalAlpha = 0.5;
                        ctx.fillRect(args.x, args.y, args.w, args.h + strH);

                        ctx.globalAlpha = 1;
                        renderer.renderCell(args2);

                        ctx.globalAlpha = 0.3;
                        ctx.fillRect(args.x, args.y, args.w, args.h + strH);

                        ctx.restore();
                    } else {
                        renderer.renderCell(args2);
                    }

                    args.viewer.paintItemBackground(args.obj, args.x, args.y + args.h, args.w, strH, 10);
                }

                y += args.h + strH;
            }

            if (visible) {
                ctx.save();

                if (selected) {
                    ctx.fillStyle = Controls.theme.treeItemSelectionForeground;
                } else {
                    ctx.fillStyle = Controls.theme.treeItemForeground;
                }


                let y2 = y - lineHeight * (lines.length - 1) - 5;

                for (const line of lines) {
                    const m = ctx.measureText(line);
                    const x2 = Math.max(x, x + args.w / 2 - m.width / 2);
                    ctx.fillText(line, x2, y2);
                    y2 += lineHeight;
                }
                ctx.restore();
            }

            return strH;
        }

    }


}