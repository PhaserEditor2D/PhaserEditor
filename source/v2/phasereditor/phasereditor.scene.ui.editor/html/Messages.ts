namespace PhaserEditor2D {

    export class BuildMessage {

        static SetTileSpriteProperties(objects: Phaser.GameObjects.GameObject[]) {
            const list = [];

            for (let obj of objects) {
                const sprite = <Phaser.GameObjects.TileSprite>obj;
                const data = { id: sprite.name };
                TileSpriteComponent.updateData(sprite, data);
                list.push(data);
            }

            return {
                method: "SetTileSpriteProperties",
                list: list
            };
        }

        static SetOriginProperties(objects: any[]) {
            const list = [];

            for (let obj of objects) {
                const data = { id: obj.name };
                
                OriginComponent.updateData(obj, data);
                TransformComponent.set_x(data, obj.x);
                TransformComponent.set_y(data, obj.y);

                list.push(data);
            }

            return {
                method: "SetOriginProperties",
                list: list
            };
        }
    }
}
