
namespace phasereditor2d.ui.ide.editors.scene {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;

    export class SceneMaker {

        private _scene: GameScene;

        constructor(scene: GameScene) {
            this._scene = scene;
        }

        createObject(objData: any) {

            const reader = new json.SceneParser(this._scene);

            return reader.createObject(objData);
        }

        createContainerWithObjects(objects: Phaser.GameObjects.GameObject[]) {

            const container = this._scene.add.container(0, 0, objects);

            const name = this._scene.sys.displayList.makeNewName("container");

            container.setEditorLabel(name);

            json.SceneParser.setNewId(container);

            return container;
        }

        async createWithDropEvent_async(e: DragEvent, dropDataArray: any[]) {

            const nameMaker = new ide.utils.NameMaker(obj => {
                return (<Phaser.GameObjects.GameObject>obj).getEditorLabel();
            });

            this._scene.sys.displayList.visit(obj => nameMaker.update([obj]));

            const worldPoint = this._scene.getCamera().getWorldPoint(e.offsetX, e.offsetY);
            const x = worldPoint.x;
            const y = worldPoint.y;

            const parser = new json.SceneParser(this._scene);

            for (const data of dropDataArray) {
                await parser.addToCache_async(data);
            }

            const sprites: Phaser.GameObjects.GameObject[] = [];

            for (const data of dropDataArray) {

                if (data instanceof pack.AssetPackImageFrame) {

                    const sprite = this._scene.add.image(x, y, data.getPackItem().getKey(), data.getName());

                    sprite.setEditorLabel(nameMaker.makeName(data.getName()));
                    sprite.setEditorTexture(data.getPackItem().getKey(), data.getName());

                    sprites.push(sprite);

                } else if (data instanceof pack.AssetPackItem) {

                    switch (data.getType()) {
                        case pack.IMAGE_TYPE: {

                            const sprite = this._scene.add.image(x, y, data.getKey());

                            sprite.setEditorLabel(nameMaker.makeName(data.getKey()));
                            sprite.setEditorTexture(data.getKey(), null);

                            sprites.push(sprite);

                            break;
                        }
                    }
                }
            }

            for (const sprite of sprites) {

                sprite.setEditorScene(this._scene);

                json.SceneParser.setNewId(sprite);
                json.SceneParser.initSprite(sprite);
            }

            return sprites;
        }

    }

}