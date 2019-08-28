/// <reference path="../../../phasereditor2d.ui.controls/PaddingPanel.ts"/>

namespace phasereditor2d.ui.ide {
    
    export class DesignWindow extends controls.PaddingPane {

        private _outlineView: ui.outline.OutlineView;
        private _filesView: ui.files.FilesViewer;
        private _inspectorView: ui.inspector.InspectorView;
        private _blocksView: ui.blocks.BlocksView;
        private _editorArea: ide.EditorArea;
        private _toolbar: ui.toolbar.Toolbar;
        private _split_Files_Blocks: controls.SplitPanel;
        private _split_Editor_FilesBlocks: controls.SplitPanel;
        private _split_Outline_EditorFilesBlocks: controls.SplitPanel;
        private _split_OutlineEditorFilesBlocks_Inspector: controls.SplitPanel;

        constructor() {
            super();

            this._toolbar = new toolbar.Toolbar();
            this._outlineView = new outline.OutlineView();
            this._filesView = new files.FilesViewer();
            this._inspectorView = new inspector.InspectorView();
            this._blocksView = new blocks.BlocksView();
            this._editorArea = new ide.EditorArea();

            this._split_Files_Blocks = new controls.SplitPanel(this._filesView, this._blocksView);
            this._split_Editor_FilesBlocks = new controls.SplitPanel(this._editorArea, this._split_Files_Blocks, false);
            this._split_Outline_EditorFilesBlocks = new controls.SplitPanel(this._outlineView, this._split_Editor_FilesBlocks);
            this._split_OutlineEditorFilesBlocks_Inspector = new controls.SplitPanel(this._split_Outline_EditorFilesBlocks, this._inspectorView);
            this.setControl(this._split_OutlineEditorFilesBlocks_Inspector);

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

        getOutlineView() {
            return this._outlineView;
        }

        getFilesView() {
            return this._filesView;
        }

        getBlocksView() {
            return this._blocksView;
        }

        getInspectorView() {
            return this._inspectorView;
        }

        getEditorArea() {
            return this._editorArea;
        }
    }
}