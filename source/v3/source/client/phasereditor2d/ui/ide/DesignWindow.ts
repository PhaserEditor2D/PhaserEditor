/// <reference path="../ide/Window.ts"/>

namespace phasereditor2d.ui.ide {

    export class DesignWindow extends ide.Window {

        private _outlineView: outline.OutlineView;
        private _filesView: views.files.FilesView;
        private _inspectorView: inspector.InspectorView;
        private _blocksView: blocks.BlocksView;
        private _editorArea: ide.EditorArea;
        private _toolbar: toolbar.Toolbar;
        private _split_Files_Blocks: controls.SplitPanel;
        private _split_Editor_FilesBlocks: controls.SplitPanel;
        private _split_Outline_EditorFilesBlocks: controls.SplitPanel;
        private _split_OutlineEditorFilesBlocks_Inspector: controls.SplitPanel;

        public constructor() {
            super();

            this._outlineView = new outline.OutlineView();
            this._filesView = new views.files.FilesView();
            this._inspectorView = new inspector.InspectorView();
            this._blocksView = new blocks.BlocksView();
            this._editorArea = new ide.EditorArea();

            this._split_Files_Blocks = new controls.SplitPanel(this.createViewFolder(this._filesView), this.createViewFolder(this._blocksView));
            this._split_Editor_FilesBlocks = new controls.SplitPanel(this._editorArea, this._split_Files_Blocks, false);
            this._split_Outline_EditorFilesBlocks = new controls.SplitPanel(this.createViewFolder(this._outlineView), this._split_Editor_FilesBlocks);
            this._split_OutlineEditorFilesBlocks_Inspector = new controls.SplitPanel(this._split_Outline_EditorFilesBlocks, this.createViewFolder(this._inspectorView));
            this.add(this._split_OutlineEditorFilesBlocks_Inspector);

            window.addEventListener("resize", e => {
                this.setBoundsValues(0, 0, window.innerWidth, window.innerHeight);
            });

            this.initialLayout();
        }

        private initialLayout() {
            const b = { x: 0, y: 0, width: window.innerWidth, height: window.innerHeight };

            this._split_Files_Blocks.setSplitFactor(0.2);
            this._split_Editor_FilesBlocks.setSplitFactor(0.6);
            this._split_Outline_EditorFilesBlocks.setSplitFactor(0.15);
            this._split_OutlineEditorFilesBlocks_Inspector.setSplitFactor(0.8);

            this.setBounds(b);
        }
    }
}