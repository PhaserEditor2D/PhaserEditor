/// <reference path="./TreeViewerLayout.ts" />

namespace phasereditor2d.ui.controls.viewers {

    const GRID_PADDING = 5;

    export class GridTreeRenderer extends TreeViewerRenderer {

        constructor(viewer : TreeViewer) {
            super(viewer)
        }

        protected paintItems(objects: any[], treeIconList: TreeIconInfo[], paintItems: PaintItem[], x: number, y: number) {

            const viewer = this.getViewer();

            const context = viewer.getContext();

            const b = viewer.getBounds();

            for (let obj of objects) {

                const children = viewer.getContentProvider().getChildren(obj);
                const expanded = viewer.isExpanded(obj);

                if (viewer.isFilterIncluded(obj)) {

                    const renderer = viewer.getCellRendererProvider().getCellRenderer(obj);

                    const args = new RenderCellArgs(context, x, y, 0, 0, obj, viewer);
                    const cellHeight = renderer.cellHeight(args);

                    args.h = cellHeight;
                    args.w = cellHeight;

                    viewer.paintItemBackground(obj, x, y, args.w, args.h);

                    if (y > -viewer.getCellSize() && y < b.height) {

                        x += GRID_PADDING;

                        this.renderCell(args, renderer);

                        // render tree icon
                        if (children.length > 0) {
                            const iconY = y + (cellHeight - TREE_ICON_SIZE) / 2;

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

                    x += cellHeight;

                    if (x + GRID_PADDING + cellHeight > b.width) {
                        y += cellHeight + GRID_PADDING;
                        x = 0;
                    }
                }

                if (expanded) {
                    const result = this.paintItems(children, treeIconList, paintItems, x, y);
                    y = result.y;
                    x = result.x;
                }
            }

            return {
                x : x,
                y : y
            };
        }

    }
}