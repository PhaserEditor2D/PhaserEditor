/// <reference path="./gameobjects/EditorImage.ts" />
/// <reference path="./gameobjects/EditorContainer.ts" />

namespace Phaser.GameObjects {

    interface EditorTexture {

        setEditorTexture(key: string, frame: string): void;

        getEditorTexture(): { key: string, frame: any };

    }

    export interface Image extends EditorTexture {

    }
}

namespace phasereditor2d.scene.ui {

    // Image

    Phaser.GameObjects.Image.prototype.setEditorTexture = function (key: string, frame: any) {
        this.setData("textureKey", key);
        this.setData("textureFrameKey", frame);
    };


    Phaser.GameObjects.Image.prototype.getEditorTexture = function () {
        return {
            key: this.getData("textureKey"),
            frame: this.getData("textureFrameKey")
        };
    };

}