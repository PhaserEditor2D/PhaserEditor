// Container

Phaser.GameObjects.Container.prototype.writeJSON = function (data: any) {

    data.type = "Container";

    phasereditor2d.ui.ide.editors.scene.json.ContainerComponent.write(this, data);

};

Phaser.GameObjects.Container.prototype.readJSON = function (data: any) {

    phasereditor2d.ui.ide.editors.scene.json.ContainerComponent.read(this, data);

};


// Image

Phaser.GameObjects.Image.prototype.writeJSON = function (data: any) {

    data.type = "Image";

    phasereditor2d.ui.ide.editors.scene.json.ImageComponent.write(this, data);

};

Phaser.GameObjects.Image.prototype.readJSON = function (data: any) {

    phasereditor2d.ui.ide.editors.scene.json.ImageComponent.read(this, data);

};

namespace Phaser.GameObjects {

    export interface ReadWriteJSON {

        writeJSON(data: any): void;

        readJSON(data: any): void;
    }

    export interface GameObject extends ReadWriteJSON {

    }

}