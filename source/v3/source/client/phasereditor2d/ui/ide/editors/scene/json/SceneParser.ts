
namespace phasereditor2d.ui.ide.editors.scene.json {

    let SPRITE_ID = 0;

    export class SceneParser {

        private _scene: GameScene;

        constructor(scene: GameScene) {
            this._scene = scene;
        }

        createScene(data: SceneData): void {

            this.createSceneCache(data);

            for (const objData of data.displayList) {
                this.createObject(objData);
            }
        }

        createSceneCache(data: SceneData) {

            for (const objData of data.displayList) {
                const type = objData.type;
                switch (type) {
                    case "Image":
                        const key = data[TextureComponent.textureKey];
                        const frame = data[TextureComponent.frameKey];

                        break;
                }
            }

        }

        addToCache(data: pack.AssetPackItem | pack.AssetPackImageFrame) {

            let imageFrameContainerPackItem: pack.AssetPackItem = null;

            if (data instanceof pack.AssetPackItem && data.getType() === pack.IMAGE_TYPE) {
                imageFrameContainerPackItem = data;
            } else if (data instanceof pack.AssetPackImageFrame) {
                imageFrameContainerPackItem = data.getPackItem();
            }

            if (imageFrameContainerPackItem !== null) {

                const parser = pack.AssetPackUtils.getImageFrameParser(imageFrameContainerPackItem);
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

                sprite.setInteractive();

            }

            return sprite;
        }

        static setNewId(sprite: Phaser.GameObjects.GameObject) {
            sprite.name = (SPRITE_ID++).toString();
        }

    }

}