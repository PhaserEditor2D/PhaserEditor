/// <reference path="../viewers/AssetPackContentProvider.ts" />

namespace phasereditor2d.pack.ui.editor {

    export class AssetPackEditorContentProvider extends viewers.AssetPackContentProvider {

        private _editor: AssetPackEditor;

        constructor(editor: AssetPackEditor) {
            super();

            this._editor = editor;
        }


        getPack() {
            return this._editor.getPack();
        }

        getRoots(input: any): any[] {

            if (this.getPack()) {
                return this.getPack().getItems();
            }
            
            return [];
        }

        getChildren(parent: any): any[] {

            if (typeof (parent) === "string") {
                const type = parent;

                if (this.getPack()) {

                    const children =
                        this.getPack().getItems()
                            .filter(item => item.getType() === type);

                    return children;
                }
            }

            return super.getChildren(parent);
        }

    }

}