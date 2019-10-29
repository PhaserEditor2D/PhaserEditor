namespace phasereditor2d.pack.ui.editor.undo {

    import ide = colibri.ui.ide;

    export class ChangeItemFieldOperation extends ide.undo.Operation {

        private _editor: AssetPackEditor;
        private _itemIndexList: number[];
        private _fieldKey: string;
        private _newValueList: any[];
        private _oldValueList: any[];
        private _updateSelection: boolean;

        constructor(editor: AssetPackEditor, items: core.AssetPackItem[], fieldKey: string, newValue: any, updateSelection: boolean = false) {
            super();

            this._editor = editor;
            this._itemIndexList = items.map(item => this._editor.getPack().getItems().indexOf(item));
            this._fieldKey = fieldKey;
            this._updateSelection = updateSelection;

            this._newValueList = [];

            this._oldValueList = items.map(item => item.getData()[fieldKey]);

            for (let i = 0; i < items.length; i++) {
                this._newValueList.push(newValue);
            }

            this.load(this._newValueList);
        }

        undo(): void {
            this.load(this._oldValueList);
        }

        redo(): void {
            this.load(this._newValueList);
        }

        private load(values: any[]) {

            for (let i = 0; i < this._itemIndexList.length; i++) {

                const index = this._itemIndexList[i];

                const item = this._editor.getPack().getItems()[index];

                item.getData()[this._fieldKey] = values[i];

            }

            this._editor.repaintEditorAndOutline();

            this._editor.setDirty(true);

            if (this._updateSelection) {
                this._editor.setSelection(this._editor.getSelection());
            }
        }
    }
}