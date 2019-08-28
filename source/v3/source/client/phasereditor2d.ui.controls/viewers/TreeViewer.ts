/// <reference path="./Viewer.ts"/>

namespace phasereditor2d.ui.controls.viewers {

    const TREE_ICON_SIZE = 16;
    const LABEL_MARGIN = TREE_ICON_SIZE + 0;


    declare type TreeIconInfo = {
        rect: Rect,
        obj: any
    }

    export class TreeViewer extends Viewer {

        private _treeIconList: TreeIconInfo[];

        constructor() {
            super();
            this._treeIconList = [];

            this.getCanvas().addEventListener("click", e => this.onClick(e));
        }

        private onClick(e: MouseEvent) {
            for (let icon of this._treeIconList) {
                if (icon.rect.contains(e.offsetX, e.offsetY)) {
                    this.setExpanded(icon.obj, !this.isExpanded(icon.obj));
                    this.repaint();
                    return;
                }
            }
        }

        visitObjects(visitor: Function) {
            const list = this.getContentProvider().getRoots(this.getInput());
            this.visitObjects2(list, visitor);
        }

        private visitObjects2(objects: any[], visitor: Function) {
            for (var obj of objects) {
                visitor(obj);
                if (this.isExpanded(obj)) {
                    const list = this.getContentProvider().getChildren(obj);
                    this.visitObjects2(list, visitor);
                }
            }
        }

        async preload() {
            const list: Promise<any>[] = [];
            this.visitObjects(obj => {
                var renderer = this.getCellRendererProvider().getCellRenderer(obj);
                list.push(renderer.preload(obj));
            });
            return Promise.all(list);
        }


        protected paint(): void {
            let x = 0;
            let y = this.getScrollY();

            this._treeIconList = [];

            // TODO: missing taking the scroll offset to compute the non-painting area

            const contentProvider = this.getContentProvider();

            const roots = contentProvider.getRoots(this.getInput());

            this._contentHeight = this.paintItems(roots, x, y) - this.getScrollY();
        }

        private paintItems(objects: any[], x: number, y: number): number {
            const b = this.getBounds();

            for (let obj of objects) {
                const children = this.getContentProvider().getChildren(obj);
                const expanded = this.isExpanded(obj);
                const renderer = this.getCellRendererProvider().getCellRenderer(obj);

                const args = new RenderCellArgs(this._context, x + LABEL_MARGIN, y, obj, this);

                const cellHeight = renderer.cellHeight(args);

                super.paintItemBackground(obj, 0, y, b.width, cellHeight);

                if (y > -this.getCellSize() && y < b.height /* + scrollOffset */) {

                    // render tree icon
                    if (children.length > 0) {
                        const iconY = y + (cellHeight - TREE_ICON_SIZE) / 2;

                        const icon = Controls.getIcon(expanded ? Controls.ICON_TREE_COLLAPSE : Controls.ICON_TREE_EXPAND);
                        icon.paint(this._context, x, iconY);

                        this._treeIconList.push({
                            rect: new Rect(x, iconY, TREE_ICON_SIZE, TREE_ICON_SIZE),
                            obj: obj
                        });
                    }

                    // client render cell
                    renderer.renderCell(args);

                    const item = new PaintItem(this._paintItems.length, obj);
                    item.set(args.x, args.y, b.width, cellHeight);
                    this._paintItems.push(item);
                }

                y += cellHeight;

                if (expanded) {
                    y = this.paintItems(children, x + LABEL_MARGIN, y);
                }
            }

            return y;
        }

        getContentProvider(): ITreeContentProvider {
            return <ITreeContentProvider>super.getContentProvider();
        }

        setContentProvider(contentProvider: ITreeContentProvider): void {
            super.setContentProvider(contentProvider);
        }
    }
}