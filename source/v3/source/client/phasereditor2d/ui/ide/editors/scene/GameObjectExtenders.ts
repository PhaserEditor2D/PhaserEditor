namespace Phaser.GameObjects {

    export interface GetScreenBounds {
        getScreenBounds(camera: Phaser.Cameras.Scene2D.Camera): Phaser.Math.Vector2[];
    }

    export interface GameObject extends GetScreenBounds { }
}

for (const proto of [
    Phaser.GameObjects.Image.prototype,
    Phaser.GameObjects.TileSprite.prototype,
    Phaser.GameObjects.BitmapText.prototype,
    Phaser.GameObjects.Text.prototype
]) {
    proto.getScreenBounds = function (camera: Phaser.Cameras.Scene2D.Camera) {
        return phasereditor2d.ui.ide.editors.scene.getScreenBounds(this, camera);
    }
}

namespace phasereditor2d.ui.ide.editors.scene {

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