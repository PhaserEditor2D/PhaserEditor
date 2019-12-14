namespace phasereditor2d.scene.ui.gameobjects {

    export class EditorContainer extends Phaser.GameObjects.Container implements EditorObject {

        static add(scene: Phaser.Scene, x: number, y: number, list: EditorObject[]) {

            const container = new EditorContainer(scene, x, y, list);

            scene.sys.displayList.add(container);

            return container;
        }

        get list(): EditorObject[] {
            return <any>super.list;
        }

        set list(list : EditorObject[]) {
            super.list = list;
        }

        writeJSON(data: any) {

            data.type = "Container";

            json.ContainerComponent.write(this, data);
        };

        readJSON(data: any) {

            json.ContainerComponent.read(this, data);
        };
    }
}