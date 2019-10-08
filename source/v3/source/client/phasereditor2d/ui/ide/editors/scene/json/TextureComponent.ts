namespace phasereditor2d.ui.ide.editors.scene.json {

    import write = core.json.write;
    import read = core.json.read;

    export class TextureComponent {

        static write(sprite: Phaser.GameObjects.Image, data: any): void {

            const texture = sprite.getEditorTexture();

            write(data, "textureKey", texture.key);
            write(data, "frameKey", texture.frame);
        }

        static read(sprite: Phaser.GameObjects.Image, data: any): void {

            const key = read(data, "textureKey");
            const frame = read(data, "frameKey");

            sprite.setEditorTexture(key, frame);
            sprite.setTexture(key, frame);
        }

    }


}