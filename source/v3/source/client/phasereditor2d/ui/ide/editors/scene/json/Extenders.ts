Phaser.GameObjects.Image.prototype.writeJSON = function (data: any) {
    data.type = "Image";

    const json = phasereditor2d.ui.ide.editors.scene.json;

    json.Object_write(this, data);

    json.Variable_write(this, data);

    json.Transform_write(this, data);

    json.Texture_write(this, data);
};

Phaser.GameObjects.Image.prototype.readJSON = function (data: any) {

    const json = phasereditor2d.ui.ide.editors.scene.json;

    json.Object_read(this, data);

    json.Variable_read(this, data);

    json.Transform_read(this, data);

    json.Texture_read(this, data);
};

namespace Phaser.GameObjects {

    export interface ReadWriteJSON {

        writeJSON(data: any): void;

        readJSON(data: any): void;
    }

    export interface GameObject extends ReadWriteJSON {

    }

}