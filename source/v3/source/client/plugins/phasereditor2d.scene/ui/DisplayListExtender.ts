namespace Phaser.GameObjects {

    export interface DisplayList {

        getByEditorId(id: string): GameObject;

        visit(visitor: (obj: GameObject) => void);

        makeNewName(baseName: string): string;
    }

}

namespace phasereditor2d.scene.ui {

    Phaser.GameObjects.DisplayList.prototype.visit = function (visitor: (obj: Phaser.GameObjects.GameObject) => void) {

        for (const obj of this.list) {
            phasereditor2d.scene.ui.runObjectVisitor(obj, visitor);
        }

    }

    Phaser.GameObjects.DisplayList.prototype.makeNewName = function (baseName: string) {

        const nameMaker = new colibri.ui.ide.utils.NameMaker((obj: gameobjects.EditorObject) => {
            return obj.getEditorLabel();
        });

        this.visit(obj => nameMaker.update([obj]));

        return nameMaker.makeName(baseName);
    }

    export function runObjectVisitor(obj: Phaser.GameObjects.GameObject, visitor: (obj: Phaser.GameObjects.GameObject) => void) {
        visitor(obj);

        if (obj instanceof Phaser.GameObjects.Container) {
            for (const child of obj.list) {
                visitor(child);
            }
        }
    }

}