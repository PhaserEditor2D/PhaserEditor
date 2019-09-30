/// <reference path="../pack/viewers/AssetPackContentProvider.ts" />

namespace phasereditor2d.ui.ide.editors.scene {

    const SCENE_EDITOR_BLOCKS_PACK_ITEM_TYPES = new Set(["image", "atlas", "atlasXML", "multiatlas", "unityAtlas", "spritesheet"]);

    export class SceneEditorBlocksContentProvider extends pack.viewers.AssetPackContentProvider {
        private _items: pack.AssetPackItem[];

        constructor(packs: pack.AssetPack[]) {
            super();

            this._items = packs
                .flatMap(pack => pack.getItems())
                .filter(item => SCENE_EDITOR_BLOCKS_PACK_ITEM_TYPES.has(item.getType()));
        }

        getItems() {
            return this._items;
        }

        getRoots(input: any): any[] {
            return this._items;
        }

        getChildren(parent: any): any[] {
            return super.getChildren(parent);
        }
    }
}