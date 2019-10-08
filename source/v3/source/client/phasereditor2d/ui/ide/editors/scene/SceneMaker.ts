namespace phasereditor2d.ui.ide.editors.scene {

    export class SceneMaker {

        private _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            this._editor = editor;
        }

        createObject(objData: any) {
            const reader = new json.SceneParser(this._editor.getGameScene());
            reader.createObject(objData);
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

            const parser = new json.SceneParser(scene);

            for (const data of dropDataArray) {
                parser.addToCache(data);
            }

            const sprites: Phaser.GameObjects.GameObject[] = [];

            for (const data of dropDataArray) {

                if (data instanceof pack.AssetPackImageFrame) {

                    const sprite = scene.add.image(x, y, data.getPackItem().getKey(), data.getName());

                    sprite.setEditorLabel(nameMaker.makeName(data.getName()));
                    sprite.setEditorTexture(data.getPackItem().getKey(), data.getName());

                    sprites.push(sprite);

                } else if (data instanceof pack.AssetPackItem) {

                    switch (data.getType()) {
                        case pack.IMAGE_TYPE: {

                            const sprite = scene.add.image(x, y, data.getKey());

                            sprite.setEditorLabel(nameMaker.makeName(data.getKey()));
                            sprite.setEditorTexture(data.getKey(), null);

                            sprites.push(sprite);

                            break;
                        }
                    }
                }
            }

            for (const sprite of sprites) {
                json.SceneParser.setNewId(sprite);
                json.SceneParser.initSprite(sprite);
            }

            this._editor.setSelection(sprites);

            this._editor.repaint();

            return sprites;
        }

    }

}