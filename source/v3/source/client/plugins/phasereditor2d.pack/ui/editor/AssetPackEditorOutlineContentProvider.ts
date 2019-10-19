namespace phasereditor2d.pack.ui.editor {

    export class AssetPackEditorOutlineContentProvider extends AssetPackEditorContentProvider {


        constructor(editor: AssetPackEditor) {
            super(editor);
        }

        getRoots() {

            if (this.getPack()) {

                const types = this.getPack().getItems().map(item => item.getType());

                const set = new Set(types);

                const result = [];

                for (const type of set) {
                    result.push(type);
                }

                return result;
            }

            return [];
        }
    }

}