
namespace phasereditor2d.ui.ide.editors.scene.json {

    let SPRITE_ID = 0;

    export class SceneParser {

        private _scene: GameScene;

        constructor(scene: GameScene) {
            this._scene = scene;
        }

        createScene(data: SceneData) {

            for (const objData of data.displayList) {
                this.createObject(objData);
            }
        }

        async createSceneCache_async(data: SceneData) {

            for (const objData of data.displayList) {
                const type = objData.type;
                switch (type) {
                    case "Image": {

                        const key = objData[TextureComponent.textureKey];
                        const finder = await pack.AssetFinder.create();
                        const item = finder.findAssetPackItem(key);

                        if (item) {
                            await this.addToCache_async(item);
                        }

                        break;
                    }
                }
            }

        }

        async addToCache_async(data: pack.AssetPackItem | pack.AssetPackImageFrame) {

            let imageFrameContainerPackItem: pack.AssetPackItem = null;

            if (data instanceof pack.AssetPackItem) {
                if (data.getType() === pack.IMAGE_TYPE) {
                    imageFrameContainerPackItem = data;
                } else if (pack.AssetPackUtils.isImageFrameContainer(data)) {
                    imageFrameContainerPackItem = data;
                }
            } else if (data instanceof pack.AssetPackImageFrame) {
                imageFrameContainerPackItem = data.getPackItem();
            }

            if (imageFrameContainerPackItem !== null) {

                const parser = pack.AssetPackUtils.getImageFrameParser(imageFrameContainerPackItem);

                await parser.preload();

                parser.addToPhaserCache(this._scene.game);

            }
        }

        createObject(data: any) {
            const type = data.type;

            let sprite: Phaser.GameObjects.GameObject = null;

            switch (type) {
                case "Image":
                    sprite = this._scene.add.image(0, 0, "");
                    break;
            }

            if (sprite) {

                sprite.readJSON(data);

                SceneParser.setNewId(sprite);

                SceneParser.initSprite(sprite);

            }

            return sprite;
        }

        static initSprite(sprite: Phaser.GameObjects.GameObject) {
            sprite.setInteractive();
        }

        static setNewId(sprite: Phaser.GameObjects.GameObject) {
            sprite.name = (SPRITE_ID++).toString();
        }

    }

}