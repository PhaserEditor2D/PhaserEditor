namespace phasereditor2d.scene.ui.json {

    export class ContainerComponent {

        static write(container : Phaser.GameObjects.Container, data : any) {

            const sprite : any = container;

            ObjectComponent.write(sprite, data);

            VariableComponent.write(sprite, data);

            TransformComponent.write(sprite, data);

            // container

            data.list = container.list.map(obj => {

                const objData = {};

                obj.writeJSON(objData);

                return objData;
            });

        }


        static read(container : Phaser.GameObjects.Container, data : any) {
            
            const sprite : any = container;

            ObjectComponent.read(sprite, data);

            VariableComponent.read(sprite, data);

            TransformComponent.read(sprite, data);

            // container

            const parser = new SceneParser(container.getEditorScene());

            for(const objData of data.list) {
                const sprite = parser.createObject(objData);    
                container.add(sprite);
            }

        }
    }

}