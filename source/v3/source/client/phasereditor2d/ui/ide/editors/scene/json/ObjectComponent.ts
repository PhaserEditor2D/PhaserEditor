namespace phasereditor2d.ui.ide.editors.scene.json {

    import write = core.json.write;
    import read = core.json.read;

    export class ObjectComponent {

        static write(sprite: Phaser.GameObjects.Image, data: any): void {
            write(data, "name", sprite.name);
        }

        static read(sprite: Phaser.GameObjects.Image, data: any): void {
            sprite.name = read(data, "name");
        }

    }

}