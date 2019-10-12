namespace phasereditor2d.scene.ui.json {

    import write = colibri.core.json.write;
    import read = colibri.core.json.read;

    export class ObjectComponent {

        static write(sprite: Phaser.GameObjects.Image, data: any): void {
            write(data, "id", sprite.getEditorId());
            write(data, "type", sprite.type);
        }

        static read(sprite: Phaser.GameObjects.Image, data: any): void {
            sprite.setEditorId(read(data, "id"));
        }

    }

}