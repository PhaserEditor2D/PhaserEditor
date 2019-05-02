namespace PhaserEditor2D {
    export class HandTool extends InteractiveTool {
        private _dragStartPoint: Phaser.Math.Vector2;
        private _dragStartCameraScroll: Phaser.Math.Vector2;
        private _dragging: boolean;

        constructor() {
            super();
        }

        canEdit(obj: any): boolean {
            return true;
        }

        containsPointer() {
            return true;
        }

        update() {
            
        }

        isEditing() {
            return this._dragging;
        }

        onMouseDown() {
            this._dragging = true;

            const pointer = this.getToolPointer();
            this._dragStartPoint = new Phaser.Math.Vector2(pointer.x, pointer.y);
            const cam = this.objScene.cameras.main;
            this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);
        }

        onMouseMove() {
            if (this._dragging) {
                this.objScene.input.setDefaultCursor("grabbing");
                const pointer = this.getToolPointer();

                const dx = this._dragStartPoint.x - pointer.x;
                const dy = this._dragStartPoint.y - pointer.y;

                const cam = this.objScene.cameras.main;

                cam.scrollX = this._dragStartCameraScroll.x + dx / cam.zoom;
                cam.scrollY = this._dragStartCameraScroll.y + dy / cam.zoom;

                Editor.getInstance().repaint();
            }
        }

        onMouseUp() {
            if (this._dragging) {
                this._dragging = false;
                this.objScene.input.setDefaultCursor("grab");
                this.objScene.sendRecordCameraStateMessage();
            }
        }

        activated() {
            this.objScene.input.setDefaultCursor("grab");
        }

        clear() {
            this.objScene.input.setDefaultCursor("default");
        }

    }
}