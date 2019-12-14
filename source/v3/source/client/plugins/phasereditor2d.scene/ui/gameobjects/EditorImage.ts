/// <reference path="./EditorObjectMixin.ts" />

namespace phasereditor2d.scene.ui.gameobjects {

    export class EditorImage extends Phaser.GameObjects.Image implements EditorObject {

        static add(scene: Phaser.Scene, x: number, y: number, texture: string, frame?: string | number) {

            const sprite = new EditorImage(scene, x, y, texture, frame);

            scene.sys.displayList.add(sprite);

            return sprite;
        }

        writeJSON(data: any) {

            data.type = "Image";

            json.ImageComponent.write(this, data);
        };

        readJSON(data: any) {

            json.ImageComponent.read(this, data);
        };

        getScreenBounds(camera: Phaser.Cameras.Scene2D.Camera) {
            return getScreenBounds(this, camera);
        }

        setEditorTexture(key: string, frame: any) {
            this.setData("textureKey", key);
            this.setData("textureFrameKey", frame);
        };

        getEditorTexture() {
            return {
                key: this.getData("textureKey"),
                frame: this.getData("textureFrameKey")
            };
        };
    }

    export interface EditorImage extends EditorObjectMixin {

    }

    colibri.lang.applyMixins(EditorImage, [EditorObjectMixin]);
}