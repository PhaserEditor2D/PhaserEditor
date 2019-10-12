namespace phasereditor2d.ui.ide.editors.scene.outline {

    import controls = colibri.ui.controls;

    export class SceneEditorOutlineLabelProvider implements controls.viewers.ILabelProvider {

        getLabel(obj: any): string {

            if (obj instanceof Phaser.GameObjects.GameObject) {
                return obj.getEditorLabel();
            }

            if (obj instanceof Phaser.GameObjects.DisplayList) {
                return "Display List";
            }

            return "" + obj;
        }

    }

}