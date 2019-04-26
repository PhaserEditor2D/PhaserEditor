namespace PhaserEditor2D {

    export class BuildMessage {

        static SetTileSpriteProperties(objects : Phaser.GameObjects.GameObject[]) {
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
    }
}