namespace phasereditor2d.scene.ui.json {

    export class ImageComponent {

        static write(sprite: gameobjects.EditorImage, data: any): void {

            ObjectComponent.write(sprite, data);

            VariableComponent.write(sprite, data);

            TransformComponent.write(sprite, data);

            TextureComponent.write(sprite, data);

        }

        static read(sprite: gameobjects.EditorImage, data: any): void {

            ObjectComponent.read(sprite, data);

            VariableComponent.read(sprite, data);

            TransformComponent.read(sprite, data);

            TextureComponent.read(sprite, data);
        }

    }
}