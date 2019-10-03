/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.ide.views.blocks {

    import viewers = controls.viewers;

    export class BlocksView extends ide.ViewerView {

        private _currentEditor: EditorPart;
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

            Workbench.getWorkbench().addEventListener(EVENT_EDITOR_ACTIVATED, e => this.onWorkbenchEditorActivated());
        }

        private async onWorkbenchEditorActivated() {

            const editor = Workbench.getWorkbench().getActiveEditor();

            console.log("editor " + editor.getTitle() + " activated ");

            let provider: EditorBlocksProvider = null;

            if (editor) {
                if (editor === this._currentEditor) {
                    provider = this._currentBlocksProvider;
                } else {
                    provider = editor.getBlocksProvider();
                }
            }

            if (provider) {
                this._currentBlocksProvider = provider;

                await provider.preload();

                this._viewer.setTreeRenderer(provider.getTreeViewerRenderer(this._viewer));
                this._viewer.setLabelProvider(provider.getLabelProvider());
                this._viewer.setCellRendererProvider(provider.getCellRendererProvider());
                this._viewer.setContentProvider(provider.getContentProvider());
                this._viewer.setInput(provider.getInput());

            } else {
                this._currentBlocksProvider = null;

                this._viewer.setInput(null);
                this._viewer.setContentProvider(new controls.viewers.EmptyTreeContentProvider());
            }

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