/// <reference path="./viewers.ts"/>

namespace phasereditor2d.ui.controls.viewers {

    export interface ITreeContentProvider {
        getRoots(): any[];

        getChildren(parent: any): any[];
    }

    export class TreeView extends View {

        protected paint(): void {
            let x = 0;
            let y = 0;

            // TODO: missing taking the scroll offset to compute the non-painting area

            const contentProvider = this.getContentProvider();

            const roots = contentProvider.getRoots();
            this.paintItems(roots, x, y);
        }

        private paintItems(objects: any[], x: number, y: number) {
            const b = this.getBounds();

            for (let obj of objects) {
                const children = this.getContentProvider().getChildren(obj);
                const renderer = this.getCellRendererProvider().getCellRenderer(obj);
                const args = new RenderCellArgs(this._context, x, y, obj);

                const cellHeight = renderer.cellHeight(args);

                if (y > -this.getCellSize() && y < b.height /* + scrollOffset */) {

                    if (this.isSelected(obj)) {
                        this.paintSelectionBackground(x, y, b.width, cellHeight);
                    }

                    if (children) {
                        // paint collapse/expand icon
                        args.x += 20;
                    }
                    renderer.renderCell(args);
                }

                y += cellHeight;

                if (this.isExpanded(obj)) {
                    this.paintItems(children, x, y);
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