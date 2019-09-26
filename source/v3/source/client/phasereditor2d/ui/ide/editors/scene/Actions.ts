namespace phasereditor2d.ui.ide.editors.scene {

    export abstract class PositionAction {

        protected _objects: Phaser.GameObjects.GameObject[];

        constructor(msg: any) {
            let displayList = Editor.getInstance().getObjectScene().sys.displayList;
            let list: string[] = msg.list;
            this._objects = list.map(id => displayList.getByName(id));
        }

        protected abstract runPositionAction(): void;

        run() {
            this.runPositionAction();

            let list = this._objects.map((obj: any) => {
                return {
                    id: obj.name,
                    x: obj.x,
                    y: obj.y
                };
            });

            Editor.getInstance().sendMessage({
                method: "SetObjectPosition",
                list: list
            });
        }
    }



    export class AlignAction extends PositionAction {
        private _align: string;

        constructor(msg: any) {
            super(msg);

            this._align = msg.actionData.align;
        }

        runPositionAction() {
            let editor = Editor.getInstance();

            let minX = Number.MAX_VALUE;
            let minY = Number.MAX_VALUE;
            let maxX = Number.MIN_VALUE;
            let maxY = Number.MIN_VALUE;

            let width = 0;
            let height = 0;

            let tx = new Phaser.GameObjects.Components.TransformMatrix();
            let point = new Phaser.Math.Vector2();

            if (this._objects.length === 1) {                
                minX = ScenePropertiesComponent.get_borderX(editor.sceneProperties);
                maxX = minX + ScenePropertiesComponent.get_borderWidth(editor.sceneProperties);
                minY = ScenePropertiesComponent.get_borderY(editor.sceneProperties);
                maxY = minY + ScenePropertiesComponent.get_borderHeight(editor.sceneProperties);
            } else {
                let points: Phaser.Math.Vector2[] = [];
                let objects = [];

                for (let obj of this._objects) {
                    let obj2 = <any>obj;

                    let w = obj2.width;
                    let h = obj2.height;
                    let ox = obj2.originX;
                    let oy = obj2.originY;
                    let x = -w * ox;
                    let y = -h * oy;

                    obj2.getWorldTransformMatrix(tx);


                    tx.transformPoint(x, y, point);
                    points.push(point.clone());

                    tx.transformPoint(x + w, y, point);
                    points.push(point.clone());

                    tx.transformPoint(x + w, y + h, point);
                    points.push(point.clone());

                    tx.transformPoint(x, y + h, point);
                    points.push(point.clone());
                }

                for (let point of points) {
                    minX = Math.min(minX, point.x);
                    minY = Math.min(minY, point.y);
                    maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, point.y);
                }

            }

            for (let obj of this._objects) {
                let objWidth = (<any>obj).displayWidth;
                let objHeight = (<any>obj).displayHeight;
                let objOriginX = (<any>obj).displayOriginX * (<any>obj).scaleX;
                let objOriginY = (<any>obj).displayOriginY * (<any>obj).scaleY;

                switch (this._align) {
                    case "LEFT":
                        this.setX(obj, minX + objOriginX);
                        break;
                    case "RIGHT":
                        this.setX(obj, maxX - objWidth + objOriginX);
                        break;
                    case "HORIZONTAL_CENTER":
                        this.setX(obj, (minX + maxX) / 2 - objWidth / 2 + objOriginX);
                        break;
                    case "TOP":
                        this.setY(obj, minY + objOriginY);
                        break;
                    case "BOTTOM":
                        this.setY(obj, maxY + height - objHeight + objOriginY);
                        break;
                    case "VERTICAL_CENTER":
                        this.setY(obj, (minY + maxY) / 2 - objHeight / 2 + objOriginY);
                        break;
                }
            }
        }

        private setX(obj: Phaser.GameObjects.GameObject, x: number) {
            if (obj.parentContainer) {
                let tx = obj.parentContainer.getWorldTransformMatrix();
                let point = tx.applyInverse(x, 0);
                (<any>obj).x = point.x;
            } else {
                (<any>obj).x = x;
            }
        }

        private setY(obj: Phaser.GameObjects.GameObject, y: number) {
            if (obj.parentContainer) {
                let tx = obj.parentContainer.getWorldTransformMatrix();
                let point = tx.applyInverse(0, y);
                (<any>obj).y = point.y;
            } else {
                (<any>obj).y = y;
            }
        }
    }
}