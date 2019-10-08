namespace phasereditor2d.ui.ide.editors.scene.json {

    import write = core.json.write;
    import read = core.json.read;

    export function Variable_write(sprite: Phaser.GameObjects.Image, data: any): void {
        write(data, "label", sprite.getEditorLabel());
    }

    export function Variable_read(sprite: Phaser.GameObjects.Image, data: any): void {
        sprite.setEditorLabel(read(data, "label"));
    }

    export function Object_write(sprite: Phaser.GameObjects.Image, data: any): void {
        write(data, "name", sprite.name);
    }

    export function Object_read(sprite: Phaser.GameObjects.Image, data: any): void {
        sprite.name = read(data, "name");
    }

    export function Transform_write(sprite: Phaser.GameObjects.Image, data: any): void {

        write(data, "x", sprite.x, 0);
        write(data, "y", sprite.y, 0);
        write(data, "scaleX", sprite.scaleX, 1);
        write(data, "scaleY", sprite.scaleY, 1);
        write(data, "angle", sprite.angle, 0);

    }

    export function Transform_read(sprite: Phaser.GameObjects.Image, data: any): void {

        sprite.x = read(data, "x", 0);
        sprite.y = read(data, "y", 0);
        sprite.scaleX = read(data, "scaleX", 1);
        sprite.scaleY = read(data, "scaleX", 1);
        sprite.angle = read(data, "angle", 0);

    }

    export function Texture_write(sprite: Phaser.GameObjects.Image, data: any): void {

        const texture = sprite.getEditorTexture();

        write(data, "textureKey", texture.key);
        write(data, "frameKey", texture.frame);
    }

    export function Texture_read(sprite: Phaser.GameObjects.Image, data: any): void {

        const key = read(data, "textureKey");
        const frame = read(data, "frameKey");

        sprite.setEditorTexture(key, frame);
        sprite.setTexture(key, frame);
    }

}