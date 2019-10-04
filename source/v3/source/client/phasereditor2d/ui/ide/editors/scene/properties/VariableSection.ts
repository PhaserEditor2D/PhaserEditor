/// <reference path="./SceneSection.ts" />

namespace phasereditor2d.ui.ide.editors.scene.properties {

    export class VariableSection extends SceneSection<Phaser.GameObjects.GameObject> {

        constructor(page: controls.properties.PropertyPage) {
            super(page, "SceneEditor.VariableSection", "Variable", false);
        }

        protected createForm(parent: HTMLDivElement) {
            const comp = this.createGridElement(parent, 2);

            {
                // Name

                this.createLabel(comp, "Name");
                const text = this.createText(comp);
                this.addUpdater(() => {
                    text.value = this.flatValues_StringJoin(this.getSelection().map(obj => obj.getEditorLabel()));
                });
            }
        }

        canEdit(obj: any, n: number): boolean {
            return obj instanceof Phaser.GameObjects.GameObject;
        }

        canEditNumber(n: number): boolean {
            return n === 1;
        }

    }

}