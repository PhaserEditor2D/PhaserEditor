namespace phasereditor2d.ui.ide.editors.scene {

    export class DropManager {

        private _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            this._editor = editor;

            const canvas = this._editor.getGameCanvas();
            canvas.addEventListener("dragover", e => this.onDragOver(e));
            canvas.addEventListener("drop", e => this.onDragDrop(e));
        }

        onDragDrop(e: DragEvent) {
            const dataArray = controls.Controls.getApplicationDragDataAndClean();

            if (this.acceptsDropDataArray(dataArray)) {
                console.log("drop");
                console.log(dataArray);
                return false;
            }

            return true;
        }

        private onDragOver(e: DragEvent) {
            e.preventDefault();
        }

        private acceptsDropData(data: any): boolean {
            if (data instanceof pack.AssetPackItem) {
                if (data.getType() === pack.IMAGE_TYPE) {
                    return true;
                }
            }
            return false;
        }

        private acceptsDropDataArray(dataArray: any[]) {
            if (!dataArray) {
                return false;
            }

            for (const item of dataArray) {
                if (!this.acceptsDropData(item)) {
                    return false;
                }
            }

            return true;

        }

    }

}