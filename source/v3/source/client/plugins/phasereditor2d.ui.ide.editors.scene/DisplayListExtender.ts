namespace Phaser.GameObjects {

    export interface DisplayList {

        getByEditorId(id: string): GameObject;

        visit(visitor: (obj: GameObject) => void);

        makeNewName(baseName: string): string;
    }

}

Phaser.GameObjects.DisplayList.prototype.getByEditorId = function (id: string) {

    const displayList: Phaser.GameObjects.DisplayList = this;


    const obj = phasereditor2d.ui.ide.editors.scene.getByEditorId(displayList.list, id);

    if (!obj) {
        console.error(`Object with id=${id} not found.`);
    }

    return obj;
}

Phaser.GameObjects.DisplayList.prototype.visit = function (visitor: (obj: Phaser.GameObjects.GameObject) => void) {

    for (const obj of this.list) {
        phasereditor2d.ui.ide.editors.scene.runObjectVisitor(obj, visitor);
    }

}

Phaser.GameObjects.DisplayList.prototype.makeNewName = function (baseName: string) {

    const nameMaker = new phasereditor2d.ui.ide.utils.NameMaker((obj: Phaser.GameObjects.GameObject) => {
        return obj.getEditorLabel();
    });

    this.visit(obj => nameMaker.update([obj]));

    return nameMaker.makeName(baseName);
}

namespace phasereditor2d.ui.ide.editors.scene {

    export function runObjectVisitor(obj: Phaser.GameObjects.GameObject, visitor: (obj: Phaser.GameObjects.GameObject) => void) {
        visitor(obj);

        if (obj instanceof Phaser.GameObjects.Container) {
            for (const child of obj.list) {
                visitor(child);
            }
        }
    }

    export function getByEditorId(list: Phaser.GameObjects.GameObject[], id: string) {

        for (const obj of list) {

            if (obj.getEditorId() === id) {
                return obj;
            }

            if (obj instanceof Phaser.GameObjects.Container) {
                
                const result = getByEditorId(obj.list, id);

                if (result) {
                    return result;
                }
            }
        }

        return null;
    }

}