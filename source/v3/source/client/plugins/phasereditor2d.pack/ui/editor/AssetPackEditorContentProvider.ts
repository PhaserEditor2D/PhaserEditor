/// <reference path="../viewers/AssetPackContentProvider.ts" />

namespace phasereditor2d.pack.ui.editor {

    import controls = colibri.ui.controls;

    export class AssetPackEditorContentProvider extends viewers.AssetPackContentProvider {

        private _pack: core.AssetPack;

        constructor(pack: core.AssetPack = null) {
            super();

            this._pack = pack;
        }

        getRoots(input: any): any[] {

            if (this._pack === null) {
                return [];
            }

            return this._pack.getItems();
        }

        getChildren(parent: any): any[] {

            if (typeof (parent) === "string") {
                const type = parent;

                if (this._pack) {

                    const children =
                        this._pack.getItems()
                            .filter(item => item.getType() === type);

                    return children;
                }
            }

            return super.getChildren(parent);
        }

    }

}