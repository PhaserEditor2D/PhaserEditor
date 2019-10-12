namespace Phaser.GameObjects {

    interface EditorTexture {

        setEditorTexture(key: string, frame: string): void;

        getEditorTexture(): { key: string, frame: any };

    }

    export interface GameObject {

        getEditorId(): string;

        setEditorId(id: string): void;

        getScreenBounds(camera: Phaser.Cameras.Scene2D.Camera): Phaser.Math.Vector2[];

        getEditorLabel(): string;

        setEditorLabel(label: string): void;

        getEditorScene(): phasereditor2d.scene.ui.GameScene;

        setEditorScene(scene: phasereditor2d.scene.ui.GameScene): void;

    }

    export interface Image extends EditorTexture {

    }
}

// GameObject

Phaser.GameObjects.GameObject.prototype.getEditorId = function () {
    return this.name;
};

Phaser.GameObjects.GameObject.prototype.setEditorId = function (id: string) {
    this.name = id;
};

Phaser.GameObjects.GameObject.prototype.getEditorLabel = function () {
    return this.getData("label") || "";
};

Phaser.GameObjects.GameObject.prototype.setEditorLabel = function (label: string) {
    this.setData("label", label);
};

Phaser.GameObjects.GameObject.prototype.getEditorScene = function () {
    return this.getData("editorScene");
};

Phaser.GameObjects.GameObject.prototype.setEditorScene = function (scene: phasereditor2d.scene.ui.GameScene) {
    this.setData("editorScene", scene);
};

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

// All

for (const proto of [
    Phaser.GameObjects.Image.prototype,
    Phaser.GameObjects.TileSprite.prototype,
    Phaser.GameObjects.BitmapText.prototype,
    Phaser.GameObjects.Text.prototype
]) {
    proto.getScreenBounds = function (camera: Phaser.Cameras.Scene2D.Camera) {
        return phasereditor2d.scene.ui.editor.getScreenBounds(this, camera);
    }
}

Phaser.GameObjects.Container.prototype.getScreenBounds = function (camera: Phaser.Cameras.Scene2D.Camera) {
    return phasereditor2d.scene.ui.editor.getContainerScreenBounds(this, camera);
}

namespace phasereditor2d.scene.ui.editor {

    export function getContainerScreenBounds(container: Phaser.GameObjects.Container, camera: Phaser.Cameras.Scene2D.Camera) {

        if (container.list.length === 0) {
            return [];
        }

        const minPoint = new Phaser.Math.Vector2(Number.MAX_VALUE, Number.MAX_VALUE);
        const maxPoint = new Phaser.Math.Vector2(Number.MIN_VALUE, Number.MIN_VALUE);

        for (const obj of container.list) {
            const bounds = obj.getScreenBounds(camera);
            for (const point of bounds) {
                minPoint.x = Math.min(minPoint.x, point.x);
                minPoint.y = Math.min(minPoint.y, point.y);
                maxPoint.x = Math.max(maxPoint.x, point.x);
                maxPoint.y = Math.max(maxPoint.y, point.y);
            }
        }

        return [
            new Phaser.Math.Vector2(minPoint.x, minPoint.y),
            new Phaser.Math.Vector2(maxPoint.x, minPoint.y),
            new Phaser.Math.Vector2(maxPoint.x, maxPoint.y),
            new Phaser.Math.Vector2(minPoint.x, maxPoint.y)
        ];
    }

    export function getScreenBounds(sprite: Phaser.GameObjects.Image, camera: Phaser.Cameras.Scene2D.Camera) {

        const points: Phaser.Math.Vector2[] = [
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0)
        ];

        let w = sprite.width;
        let h = sprite.height;

        if (sprite instanceof Phaser.GameObjects.BitmapText) {
            // the BitmapText.width is considered a displayWidth, it is already multiplied by the scale
            w = w / sprite.scaleX;
            h = h / sprite.scaleY;
        }

        let flipX = sprite.flipX ? -1 : 1;
        let flipY = sprite.flipY ? -1 : 1;

        if (sprite instanceof Phaser.GameObjects.TileSprite) {
            flipX = 1;
            flipY = 1;
        }

        const ox = sprite.originX;
        const oy = sprite.originY;

        const x = -w * ox * flipX;
        const y = -h * oy * flipY;

        const tx = sprite.getWorldTransformMatrix();

        tx.transformPoint(x, y, points[0]);
        tx.transformPoint(x + w * flipX, y, points[1]);
        tx.transformPoint(x + w * flipX, y + h * flipY, points[2]);
        tx.transformPoint(x, y + h * flipY, points[3]);

        return points.map(p => camera.getScreenPoint(p.x, p.y));
    }
}