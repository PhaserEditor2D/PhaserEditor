namespace phasereditor2d.ui.ide.editors.scene.json {

    import write = core.json.write;
    import read = core.json.read;

    export class VariableComponent {

        static write(sprite: Phaser.GameObjects.Image, data: any): void {
            write(data, "label", sprite.getEditorLabel());
        }

        static read(sprite: Phaser.GameObjects.Image, data: any): void {
            sprite.setEditorLabel(read(data, "label"));
        }

    }

}