namespace phasereditor2d.ui.ide.editors.scene.json {

    import write = core.json.write;
    import read = core.json.read;

    export class TransformComponent {

        static write(sprite: Phaser.GameObjects.Image, data: any): void {

            write(data, "x", sprite.x, 0);
            write(data, "y", sprite.y, 0);
            write(data, "scaleX", sprite.scaleX, 1);
            write(data, "scaleY", sprite.scaleY, 1);
            write(data, "angle", sprite.angle, 0);

        }

        static read(sprite: Phaser.GameObjects.Image, data: any): void {

            sprite.x = read(data, "x", 0);
            sprite.y = read(data, "y", 0);
            sprite.scaleX = read(data, "scaleX", 1);
            sprite.scaleY = read(data, "scaleY", 1);
            sprite.angle = read(data, "angle", 0);

        }

    }

}