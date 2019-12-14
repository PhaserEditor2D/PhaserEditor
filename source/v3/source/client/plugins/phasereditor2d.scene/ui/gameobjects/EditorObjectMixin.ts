namespace phasereditor2d.scene.ui.gameobjects {

    export class EditorObjectMixin extends Phaser.GameObjects.GameObject {

        getEditorId() {
            return this.name;
        };
    
        setEditorId(id: string) {
            this.name = id;
        };
    
        getEditorLabel() {
            return this.getData("label") || "";
        };
    
        setEditorLabel(label: string) {
            this.setData("label", label);
        };
    
        getEditorScene() {
            return this.getData("editorScene");
        };
    
        setEditorScene(scene: phasereditor2d.scene.ui.GameScene) {
            this.setData("editorScene", scene);
        };
    } 
}