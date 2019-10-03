namespace phasereditor2d.ui.ide.editors.scene {

    export class SelectionManager {
        private _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            this._editor = editor;
            const canvas = this._editor.getOverlayLayer().getCanvas();
            canvas.addEventListener("click", e => this.onMouseClick(e));
        }

        private onMouseClick(e: MouseEvent): void {
            const result = this.hitTestOfActivePointer();
            if (result) {
                const selected = result.pop();
                if (e.ctrlKey || e.metaKey) {
                    this._editor.setSelection(
                        this._editor.getSelection().filter(obj => obj !== selected)
                    );
                } else {
                    this._editor.setSelection([selected]);
                }
            } else {
                this._editor.setSelection([]);
            }
            
            this._editor.repaint();
        }

        hitTestOfActivePointer(): Phaser.GameObjects.GameObject[] {
            const scene = this._editor.getGameScene();
            const input = scene.input;

            // const real = input["real_hitTest"];
            // const fake = input["hitTest"];

            // input["hitTest"] = real;

            const result = input.hitTestPointer(scene.input.activePointer);

            // input["hitTest"] = fake;

            return result;
        }
    }

}