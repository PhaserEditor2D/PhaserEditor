/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.ide.blocks {

    import viewers = controls.viewers;

    export class BlocksView extends ide.ViewerView {

        private _selectionListener: any;
        private _activeEditor: EditorPart;
        private _currentBlocksProvider: EditorBlocksProvider;

        constructor() {
            super("blocksView");
            this.setTitle("Blocks");
            this.setIcon(Workbench.getWorkbench().getWorkbenchIcon(ICON_BLOCKS));
        }

        protected createViewer(): viewers.TreeViewer {
            return new viewers.TreeViewer();
        }

        protected createPart(): void {
            super.createPart();

            this._selectionListener = (e: CustomEvent) => this.onPartSelection();

            Workbench.getWorkbench().addEventListener(EVENT_PART_ACTIVATE, e => this.onWorkbenchPartActivate());
        }

        private onWorkbenchPartActivate() {
            const part = Workbench.getWorkbench().getActivePart();

            if (!part || part instanceof EditorPart && part !== this._activeEditor) {

                if (this._activeEditor) {
                    this._activeEditor.removeEventListener(controls.EVENT_SELECTION, this._selectionListener);
                }

                this._activeEditor = <EditorPart>part;

                this._activeEditor.addEventListener(controls.EVENT_SELECTION, this._selectionListener);

                this.onPartSelection();
            }
        }

        private async onPartSelection() {
            const provider = this._activeEditor.getBlocksProvider();

            if (!provider) {
                this._viewer.setInput(null);
                this._viewer.setContentProvider(new controls.viewers.EmptyTreeContentProvider());
                this._currentBlocksProvider = provider;
                return;
            }

            this._currentBlocksProvider = provider;

            await provider.preload();

            this._viewer.setTreeRenderer(provider.getTreeViewerRenderer(this._viewer));
            this._viewer.setLabelProvider(provider.getLabelProvider());
            this._viewer.setCellRendererProvider(provider.getCellRendererProvider());
            this._viewer.setContentProvider(provider.getContentProvider());
            this._viewer.setInput(provider.getInput());

            this._viewer.repaint();
        }

        getPropertyProvider() {
            if (this._currentBlocksProvider) {
                return this._currentBlocksProvider.getPropertySectionProvider();
            }
            return null;
        }
    }
}