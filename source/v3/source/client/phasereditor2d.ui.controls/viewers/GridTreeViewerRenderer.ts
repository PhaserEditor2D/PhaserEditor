/// <reference path="./TreeViewerRenderer.ts" />

namespace phasereditor2d.ui.controls.viewers {

    const GRID_PADDING = 5;

    export class GridTreeRenderer extends TreeViewerRenderer {

        private _center: boolean;

        constructor(viewer: TreeViewer, center: boolean = false) {
            super(viewer);
            viewer.setCellSize(128);
            this._center = center;
        }

        protected paintItems(objects: any[], treeIconList: TreeIconInfo[], paintItems: PaintItem[], x: number, y: number) {
            const viewer = this.getViewer();
            
            if (viewer.getCellSize() <= 48) {
                return super.paintItems(objects, treeIconList, paintItems, x, y + GRID_PADDING);
            }

            const b = viewer.getBounds();

            const offset = this._center ? Math.floor(b.width % (viewer.getCellSize() + GRID_PADDING) / 2) : 0;

            return this.paintItems2(objects, treeIconList, paintItems, x + offset, y + GRID_PADDING, offset);
        }

        private paintItems2(objects: any[], treeIconList: TreeIconInfo[], paintItems: PaintItem[], x: number, y: number, offset: number) {

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

                    if (y > -viewer.getCellSize() && y < b.height) {

                        this.renderGridCell(args, renderer);

                        // render tree icon
                        if (children.length > 0) {
                            const iconY = y + (cellSize - TREE_ICON_SIZE) / 2;

                            const icon = Controls.getIcon(expanded ? Controls.ICON_TREE_COLLAPSE : Controls.ICON_TREE_EXPAND);
                            icon.paint(context, x + 5, iconY);

                            treeIconList.push({
                                rect: new Rect(x, iconY, TREE_ICON_SIZE, TREE_ICON_SIZE),
                                obj: obj
                            });
                        }
                    }

                    const item = new PaintItem(paintItems.length, obj);
                    item.set(args.x, args.y, args.w, args.h);
                    paintItems.push(item);

                    x += cellSize + GRID_PADDING;

                    if (x + cellSize > b.width) {
                        y += cellSize + GRID_PADDING;
                        x = 0 + offset;
                    }
                }

                if (expanded) {
                    const result = this.paintItems(children, treeIconList, paintItems, x, y);
                    y = result.y;
                    x = result.x;
                }
            }

            return {
                x: x,
                y: y
            };
        }

        private renderGridCell(args: RenderCellArgs, renderer: ICellRenderer): void {
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
                        lines[lines.length - 1] += "..";
                        break;
                    } else {
                        lines.push("");
                        lines[lines.length - 1] = c;
                    }
                } else {
                    lines[lines.length - 1] += c;
                }
            }

            {
                const args2 = new RenderCellArgs(
                    args.canvasContext,
                    args.x + 3,
                    args.y + 3,
                    args.w - 6,
                    args.h - lines.length * lineHeight - 6,
                    args.obj,
                    args.viewer,
                    true
                );

                const strH = lines.length * lineHeight;

                if (args.viewer.isSelected(args.obj)) {
                    ctx.save();
                    
                    ctx.fillStyle = Controls.theme.treeItemSelectionBackground;
                    
                    ctx.globalAlpha = 0.5;
                    ctx.fillRect(args2.x - 3, args2.y - 3, args2.w + 6, args2.h + 6);
                    
                    ctx.globalAlpha = 1;
                    renderer.renderCell(args2);
                    
                    ctx.globalAlpha = 0.3;
                    ctx.fillRect(args2.x - 3, args2.y - 3, args2.w + 6, args2.h + 6);

                    ctx.restore();
                } else {
                    renderer.renderCell(args2);
                }

                args.viewer.paintItemBackground(args.obj, args.x, args.y + args.h - strH - 3, args.w, strH, 10);

                y += args2.h + lineHeight * lines.length;
            }

            ctx.save();

            if (args.viewer.isSelected(args.obj)) {
                ctx.fillStyle = Controls.theme.treeItemSelectionForeground;
            } else {
                ctx.fillStyle = Controls.theme.treeItemForeground;
            }

            let y2 = y - lineHeight * (lines.length - 1) - 5;

            for (const line of lines) {
                const m = ctx.measureText(line);
                ctx.fillText(line, x + args.w / 2 - m.width / 2, y2);
                y2 += lineHeight;
            }
            ctx.restore();
        }

    }


}