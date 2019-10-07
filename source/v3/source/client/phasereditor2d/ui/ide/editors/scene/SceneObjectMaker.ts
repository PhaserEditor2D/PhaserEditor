namespace phasereditor2d.ui.ide.editors.scene {

    let SPRITE_ID = 0;

    export class SceneObjectMaker {
        private _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            this._editor = editor;
        }

        createObject(data: any) {
            const scene = this._editor.getGameScene();

            const type = data.type;

            let sprite: Phaser.GameObjects.GameObject = null;

            if (type === "Image") {
                sprite = scene.add.image(0, 0, "");
            }

            if (sprite) {
                sprite.readJSON(data);
            }

            return sprite;
        }

        createWithDropEvent(e: DragEvent, dropDataArray: any[]) {

            const scene = this._editor.getGameScene();

            const nameMaker = new utils.NameMaker(obj => {
                return (<Phaser.GameObjects.GameObject>obj).getEditorLabel();
            });

            nameMaker.update(scene.sys.displayList.getChildren());

            const worldPoint = scene.getCamera().getWorldPoint(e.offsetX, e.offsetY);
            const x = worldPoint.x;
            const y = worldPoint.y;

            for (const data of dropDataArray) {
                SceneObjectMaker.updateTextureCacheWithAssetData(data, scene.game);
            }

            const sprites: Phaser.GameObjects.GameObject[] = [];

            for (const data of dropDataArray) {

                if (data instanceof pack.AssetPackImageFrame) {

                    const sprite = scene.add.image(x, y, data.getPackItem().getKey(), data.getName());

                    sprite.setEditorLabel(nameMaker.makeName(data.getName()));
                    sprite.setEditorAsset(data);

                    sprites.push(sprite);

                } else if (data instanceof pack.AssetPackItem) {

                    switch (data.getType()) {
                        case pack.IMAGE_TYPE: {

                            const sprite = scene.add.image(x, y, data.getKey());

                            sprite.setEditorLabel(nameMaker.makeName(data.getKey()));
                            sprite.setEditorAsset(data);

                            sprites.push(sprite);

                            break;
                        }
                    }
                }
            }

            for (const sprite of sprites) {
                this.initSprite(sprite);
            }

            this._editor.setSelection(sprites);

            this._editor.repaint();

            return sprites;
        }

        private initSprite(sprite: Phaser.GameObjects.GameObject) {

            sprite.name = (SPRITE_ID++).toString();
            // TODO: missing add the custom hit tests.
            sprite.setInteractive();

        }

        static updateTextureCacheWithAssetData(data: any, game: Phaser.Game) {

            let imageFrameContainerPackItem: pack.AssetPackItem = null;

            if (data instanceof pack.AssetPackItem && data.getType() === pack.IMAGE_TYPE) {
                imageFrameContainerPackItem = data;
            } else if (data instanceof pack.AssetPackImageFrame) {
                imageFrameContainerPackItem = data.getPackItem();
            }

            if (imageFrameContainerPackItem !== null) {

                const parser = pack.AssetPackUtils.getImageFrameParser(imageFrameContainerPackItem);
                parser.addToPhaserCache(game);

            }
        }

    }

}