/// <reference path="../../pack/viewers/AssetPackContentProvider.ts" />

namespace phasereditor2d.ui.ide.editors.scene.blocks {

    const SCENE_EDITOR_BLOCKS_PACK_ITEM_TYPES = new Set(["image", "atlas", "atlasXML", "multiatlas", "unityAtlas", "spritesheet"]);

    export class SceneEditorBlocksContentProvider extends pack.viewers.AssetPackContentProvider {

        getPackItems() {
            return pack.PackFinder

                .getPacks()

                .flatMap(pack => pack.getItems())

                .filter(item => SCENE_EDITOR_BLOCKS_PACK_ITEM_TYPES.has(item.getType()));
        }

        getRoots(input: any): any[] {

            const roots = [];

            roots.push(...this.getSceneFiles());

            roots.push(...this.getPackItems());

            return roots;
        }

        getSceneFiles() {
            return FileUtils.getAllFiles().filter(file => file.getExtension() === "scene");
        }

        getChildren(parent: any): any[] {
            if (typeof (parent) === "string") {

                switch (parent) {
                    case pack.ATLAS_TYPE:
                        return this.getPackItems()
                            .filter(item => pack.AssetPackUtils.isAtlasPackItem(item));

                    case PREFAB_SECTION:
                        //TODO: we need to implement the PrefabFinder
                        const files = this.getSceneFiles();
                        return files;
                }

                return this.getPackItems()
                    .filter(item => item.getType() === parent);
            }

            return super.getChildren(parent);
        }
    }
}