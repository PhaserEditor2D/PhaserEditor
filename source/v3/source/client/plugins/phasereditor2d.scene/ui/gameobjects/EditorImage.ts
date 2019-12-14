namespace phasereditor2d.scene.ui.gameobjects {

    export class EditorImage extends Phaser.GameObjects.Image implements EditorObject {

        static add(scene : Phaser.Scene, x : number, y : number, texture : string, frame? : string|number) {
            
            const sprite = new EditorImage(scene, x, y, texture, frame);

            scene.sys.displayList.add(sprite);

            return sprite;
        }

        writeJSON(data: any) {

            data.type = "Image";

            json.ImageComponent.write(this, data);
        };

        readJSON(data: any) {

            json.ImageComponent.read(this, data);
        };
    }
}