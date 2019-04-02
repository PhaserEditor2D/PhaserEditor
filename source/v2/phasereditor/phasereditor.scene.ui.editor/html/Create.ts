namespace PhaserEditor2D {
    export class Create {
        constructor() {

        }

        createWorld(add: Phaser.GameObjects.GameObjectFactory) {
            if (!Models.isReady()) {
                return;
            }

            var list = Models.displayList.children;

            for (var i = 0; i < list.length; i++) {
                var data = list[i];
                this.createObject(add, data);
            }
        }

        createObject(add: Phaser.GameObjects.GameObjectFactory, data: any) {

            var type = data["-type"];
            var obj: Phaser.GameObjects.GameObject;

            switch (type) {
                case "Image":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var key = TextureComponent.get_textureKey(data);
                    var frame = TextureComponent.get_textureFrame(data);

                    obj = add.image(x, y, key, frame);

                    break;

                case "Sprite":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var key = TextureComponent.get_textureKey(data);
                    var frame = TextureComponent.get_textureFrame(data);

                    obj = add.sprite(x, y, key, frame);

                    break;

                case "TileSprite":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var width = TileSpriteComponent.get_width(data);
                    var height = TileSpriteComponent.get_height(data);

                    var key = TextureComponent.get_textureKey(data);
                    var frame = TextureComponent.get_textureFrame(data);

                    obj = add.tileSprite(x, y, width, height, key, frame);
                    
                    break;
                case "BitmapText":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var key = BitmapTextComponent.get_fontAssetKey(data);

                    obj = add.bitmapText(x, y, key);

                    break;
            }

            this.updateObject(obj, data);
        }

        updateObject(obj: any, data: any) {
            var type = data["-type"];
            obj.name = data["-id"];


            switch (type) {
                case "Image":
                case "Sprite":
                case "TileSprite":
                case "BitmapText":
                    TransformComponent.updateObject(obj, data);
                    OriginComponent.updateObject(obj, data);
                    FlipComponent.updateObject(obj, data);
                    break;
            }

            switch (type) {
                case "TileSprite":
                    TileSpriteComponent.updateObject(obj, data);
                    break;
                case "BitmapText":
                    BitmapTextComponent.updateObject(obj, data);
                    break;
            }


        }
    }
}