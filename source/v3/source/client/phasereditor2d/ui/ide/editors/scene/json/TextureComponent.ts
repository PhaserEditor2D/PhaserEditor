namespace phasereditor2d.ui.ide.editors.scene.json {

    import write = core.json.write;
    import read = core.json.read;

    export class TextureComponent {


        static textureKey = "textureKey";
        static frameKey = "frameKey";

        static write(sprite: Phaser.GameObjects.Image, data: any): void {

            const texture = sprite.getEditorTexture();

            write(data, this.textureKey, texture.key);
            write(data, this.frameKey, texture.frame);
        }

        static read(sprite: Phaser.GameObjects.Image, data: any): void {

            const key = read(data, this.textureKey);
            const frame = read(data, this.frameKey);

            sprite.setEditorTexture(key, frame);
            sprite.setTexture(key, frame);
        }

    }


}