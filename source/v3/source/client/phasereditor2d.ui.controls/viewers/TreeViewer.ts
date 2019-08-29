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
                if (this.isExpanded(obj) || this.getFilterText() !== "") {
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

        protected buildFilterIncludeMap() {
            const roots = this.getContentProvider().getRoots(this.getInput());
            this.buildFilterIncludeMap2(roots);
        }

        private buildFilterIncludeMap2(objects: any[]): boolean {
            let result = false;

            for (const obj of objects) {
                let resultThis = this.matches(obj);

                const children = this.getContentProvider().getChildren(obj);
                const resultChildren = this.buildFilterIncludeMap2(children);
                resultThis = resultThis || resultChildren;

                if (resultThis) {
                    this._filterIncludeSet.add(obj);
                    result = true;
                }
            }

            return result;
        }

        private paintItems(objects: any[], x: number, y: number): number {

            const isFiltering = this.getFilterText() !== "";

            const b = this.getBounds();
            const filter = this.getFilterText();

            for (let obj of objects) {

                if (!this._filterIncludeSet.has(obj)) {
                    continue;
                }

                const children = this.getContentProvider().getChildren(obj);
                const expanded = this.isExpanded(obj) || isFiltering;

                const renderer = this.getCellRendererProvider().getCellRenderer(obj);

                const args = new RenderCellArgs(this._context, x + LABEL_MARGIN, y, b.width - x - LABEL_MARGIN, 0, obj, this);
                const cellHeight = renderer.cellHeight(args);
                args.h = cellHeight;

                super.paintItemBackground(obj, 0, y, b.width, cellHeight);

                if (y > -this.getCellSize() && y < b.height) {

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

                    this.renderCell(args, renderer);

                    const item = new PaintItem(this._paintItems.length, obj);
                    item.set(args.x, args.y, args.w, args.h);
                    this._paintItems.push(item);
                }

                y += cellHeight;


                if (expanded || filter !== "") {
                    y = this.paintItems(children, x + LABEL_MARGIN, y);
                }
            }

            return y;
        }

        private renderCell(args: RenderCellArgs, renderer: ICellRenderer): void {
            const label = this.getLabelProvider().getLabel(args.obj);
            let x = args.x;
            let y = args.y;

            const ctx = args.canvasContext;
            ctx.fillStyle = Controls.theme.treeItemForeground;

            let args2: RenderCellArgs;
            if (args.h <= ROW_HEIGHT) {
                args2 = new RenderCellArgs(args.canvasContext, args.x, args.y, 16, args.h, args.obj, args.view);
                x += 20;
                y += 15;
            } else {
                args2 = new RenderCellArgs(args.canvasContext, args.x, args.y, args.w, args.h - 20, args.obj, args.view);
                y += args2.h + 15;
            }

            renderer.renderCell(args2);

            ctx.save();
            if (args.view.isSelected(args.obj)) {
                ctx.fillStyle = Controls.theme.treeItemSelectionForeground;
            }
            ctx.fillText(label, x, y);
            ctx.restore();
        }

        getContentProvider(): ITreeContentProvider {
            return <ITreeContentProvider>super.getContentProvider();
        }

        setContentProvider(contentProvider: ITreeContentProvider): void {
            super.setContentProvider(contentProvider);
        }
    }
}