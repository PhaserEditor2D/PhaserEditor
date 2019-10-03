/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>

namespace phasereditor2d.ui.ide.views.blocks {

    import viewers = controls.viewers;

    export class BlocksView extends ide.ViewerView {

        private _currentEditor: EditorPart;
        private _currentBlocksProvider: EditorBlocksProvider;
        private _viewerMap : Map<EditorPart, any>;

        constructor() {
            super("blocksView");
            this.setTitle("Blocks");
            this.setIcon(Workbench.getWorkbench().getWorkbenchIcon(ICON_BLOCKS));

            this._viewerMap = new Map();
        }

        protected createViewer(): viewers.TreeViewer {
            return new viewers.TreeViewer();
        }

        protected createPart(): void {
            super.createPart();

            Workbench.getWorkbench().addEventListener(EVENT_EDITOR_ACTIVATED, e => this.onWorkbenchEditorActivated());
        }

        private async onWorkbenchEditorActivated() {

            if (this._currentEditor !== null) {
                const state = this._viewer.getState();
                this._viewerMap.set(this._currentEditor, state);
                console.log("save state");
                console.log(state);
                console.log("---");
            }

            const editor = Workbench.getWorkbench().getActiveEditor();

            let provider: EditorBlocksProvider = null;

            if (editor) {
                if (editor === this._currentEditor) {
                    provider = this._currentBlocksProvider;
                } else {
                    provider = editor.getBlocksProvider();
                }
            }

            if (provider) {
                await provider.preload();

                this._viewer.setTreeRenderer(provider.getTreeViewerRenderer(this._viewer));
                this._viewer.setLabelProvider(provider.getLabelProvider());
                this._viewer.setCellRendererProvider(provider.getCellRendererProvider());
                this._viewer.setContentProvider(provider.getContentProvider());
                this._viewer.setInput(provider.getInput());

                const state = this._viewerMap.get(editor);
                if (state) {
                    this._viewer.setState(state);
                }
            } else {
                this._viewer.setInput(null);
                this._viewer.setContentProvider(new controls.viewers.EmptyTreeContentProvider());
            }

            this._currentBlocksProvider = provider;
            this._currentEditor = editor;

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