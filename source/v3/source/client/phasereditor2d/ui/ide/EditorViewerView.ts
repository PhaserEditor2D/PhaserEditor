/// <reference path="./ViewerView.ts" />

namespace phasereditor2d.ui.ide {

    import viewers = controls.viewers;

    export abstract class EditorViewerView extends ide.ViewerView {

        private _currentEditor: EditorPart;
        private _currentViewerProvider: EditorViewerProvider;
        private _viewerMap: Map<EditorPart, any>;

        constructor(id: string) {
            super(id);

            this._viewerMap = new Map();
        }

        protected createViewer(): viewers.TreeViewer {
            return new viewers.TreeViewer();
        }

        protected createPart(): void {

            super.createPart();

            Workbench.getWorkbench().addEventListener(EVENT_EDITOR_ACTIVATED, e => this.onWorkbenchEditorActivated());
        }

        abstract getViewerProvider(editor: EditorPart): EditorViewerProvider;

        private async onWorkbenchEditorActivated() {

            if (this._currentEditor !== null) {

                const state = this._viewer.getState();
                this._viewerMap.set(this._currentEditor, state);

            }

            const editor = Workbench.getWorkbench().getActiveEditor();

            let provider: EditorViewerProvider = null;

            if (editor) {

                if (editor === this._currentEditor) {
                    provider = this._currentViewerProvider;
                } else {
                    provider = this.getViewerProvider(editor);
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

            this._currentViewerProvider = provider;
            this._currentEditor = editor;

            this._viewer.repaint();
        }

        getPropertyProvider() {

            if (this._currentViewerProvider) {
                return this._currentViewerProvider.getPropertySectionProvider();
            }

            return null;
        }
    }
}