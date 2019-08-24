/// <reference path="./viewers.ts"/>

namespace phasereditor2d.ui.controls.viewers {

    const TREE_ICON_SIZE = 16;
    const LABEL_MARGIN = TREE_ICON_SIZE + 5;

    export interface ITreeContentProvider {
        getRoots(input: any): any[];

        getChildren(parent: any): any[];
    }

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

        private onClick(e : MouseEvent) {
            for(let icon of this._treeIconList) {
                if (icon.rect.contains(e.offsetX, e.offsetY)) {
                    this.setExpanded(icon.obj, !this.isExpanded(icon.obj));
                    this.repaint();
                    return;
                }
            }
        }

        protected paint(): void {
            let x = 0;
            let y = 0;

            this._treeIconList = [];

            // TODO: missing taking the scroll offset to compute the non-painting area

            const contentProvider = this.getContentProvider();

            const roots = contentProvider.getRoots(this.getInput());

            this.paintItems(roots, x, y);
        }

        private paintItems(objects: any[], x: number, y: number) {
            const b = this.getBounds();

            for (let obj of objects) {
                const children = this.getContentProvider().getChildren(obj);
                const expanded = this.isExpanded(obj);
                const renderer = this.getCellRendererProvider().getCellRenderer(obj);

                const args = new RenderCellArgs(this._context, x + LABEL_MARGIN, y, obj, this);

                const cellHeight = renderer.cellHeight(args);

                if (y > -this.getCellSize() && y < b.height /* + scrollOffset */) {

                    if (this.isSelected(obj)) {
                        this.paintSelectionBackground(x, y, b.width, cellHeight);
                    }

                    // render tree icon
                    if (children.length > 0) {
                        const iconY = y + (cellHeight - TREE_ICON_SIZE) / 2;
                        
                        if (expanded) {
                            this._context.strokeStyle = "#000";
                            this._context.strokeRect(x, iconY, 16, 16);
                        } else {
                            this._context.fillStyle = "#000";
                            this._context.fillRect(x, iconY, 16, 16);
                        }

                        this._treeIconList.push({
                            rect: new Rect(x, iconY, TREE_ICON_SIZE, TREE_ICON_SIZE),
                            obj: obj
                        });
                    }

                    // client render cell
                    renderer.renderCell(args);
                }

                y += cellHeight;

                if (expanded) {
                    this.paintItems(children, x + LABEL_MARGIN, y);
                }
            }
        }

        getContentProvider(): ITreeContentProvider {
            return <ITreeContentProvider>super.getContentProvider();
        }

        setContentProvider(contentProvider: ITreeContentProvider): void {
            super.setContentProvider(contentProvider);
        }
    }
}