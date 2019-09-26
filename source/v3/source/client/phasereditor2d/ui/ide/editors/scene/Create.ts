namespace phasereditor2d.ui.ide.editors.scene {
    export class Create {
        private _interactive: boolean;

        constructor(interactive: boolean = true) {
            this._interactive = interactive;
        }

        createWorld(scene: Phaser.Scene, displayList: any) {
            var list = displayList.children;

            for (var i = 0; i < list.length; i++) {
                var data = list[i];
                this.createObject(scene, data);
            }
        }

        createObject(scene: Phaser.Scene, data: any) {

            var type = data["-type"];
            var obj: Phaser.GameObjects.GameObject;
            let add = scene.add;

            switch (type) {
                case "Image":
                case "Sprite":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var key = TextureComponent.get_textureKey(data);
                    var frame = TextureComponent.get_textureFrame(data);

                    obj = add.image(x, y, key, frame);

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

                case "DynamicBitmapText":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);

                    var key = BitmapTextComponent.get_fontAssetKey(data);

                    obj = add.dynamicBitmapText(x, y, key);

                    break;

                case "Text":
                    var x = TransformComponent.get_x(data);
                    var y = TransformComponent.get_y(data);
                    var text = TextualComponent.get_text(data);

                    obj = add.text(x, y, text);

                    break;
            }

            if (this._interactive) {
                switch (type) {
                    case "TileSprite":
                        if (Editor.getInstance().isWebGL()) {
                            //obj.setInteractive(TileSpriteCallback);
                            obj.setInteractive(getAlpha_RenderTexture);
                        } else {
                            obj.setInteractive(getAlpha_CanvasTexture);
                        }
                        break;
                    case "BitmapText":
                    case "DynamicBitmapText":
                        obj.setInteractive(inBounds_BitmapText);
                        break;
                    case "Text":
                        obj.setInteractive();
                        break;
                    default:
                        obj.setInteractive(getAlpha_SharedTexture);
                        break;
                }
            }

            this.updateObject(obj, data);
        }

        updateObject(obj: any, data: any) {
            var type = data["-type"];
            obj.name = data["-id"];

            VisibleComponent.updateObject(obj, data);

            switch (type) {
                case "Image":
                case "Sprite":
                case "TileSprite":
                    TextureComponent.updateObject(obj, data);
                    break;
            }

            switch (type) {
                case "Image":
                case "Sprite":
                case "TileSprite":
                case "BitmapText":
                case "DynamicBitmapText":
                case "Text":
                    GameObjectEditorComponent.updateObject(obj, data);
                    TransformComponent.updateObject(obj, data);
                    OriginComponent.updateObject(obj, data);
                    FlipComponent.updateObject(obj, data);
                    TintComponent.updateObject(obj, data);
                    break;
            }

            switch (type) {
                case "TileSprite":
                    TileSpriteComponent.updateObject(obj, data);
                    break;
                case "BitmapText":
                    BitmapTextComponent.updateObject(obj, data);
                    break;
                case "DynamicBitmapText":
                    BitmapTextComponent.updateObject(obj, data);
                    DynamicBitmapTextComponent.updateObject(obj, data);
                    break;
                case "Text":
                    TextualComponent.updateObject(obj, data);
                    TextComponent.updateObject(obj, data);
                    break;
            }
        }
    }

    function inBounds_BitmapText(hitArea: any, x: number, y: number, gameObject: Phaser.GameObjects.BitmapText) {
        // the bitmaptext width is considered a displayWidth, it is already multiplied by the scale
        let w = gameObject.width / gameObject.scaleX;
        let h = gameObject.height / gameObject.scaleY;

        return x >= 0 && y >= 0 && x <= w && y <= h;
    }

    function inBounds_TileSprite(hitArea: any, x: integer, y: integer, obj: Phaser.GameObjects.TileSprite) {
        return x >= 0 && y >= 0 && x <= obj.width && y <= obj.height;
    }

    // this is not working at this moment!
    function getAlpha_RenderTexture(hitArea: any, x: number, y: number, sprite: Phaser.GameObjects.Sprite) {
        var hitBounds = x >= 0 && y >= 0 && x <= sprite.width && y <= sprite.height;

        if (!hitBounds) {
            return false;
        }

        const scene = Editor.getInstance().getObjectScene();
        
        const renderTexture = new Phaser.GameObjects.RenderTexture(scene, 0, 0, 1, 1);
        
        const scaleX = sprite.scaleX;
        const scaleY = sprite.scaleY;
        const originX = sprite.originX;
        const originY = sprite.originY;
        const angle = sprite.angle;

        sprite.scaleX = 1;
        sprite.scaleY = 1;
        sprite.originX = 0;
        sprite.originY = 0;
        sprite.angle = 0;

        renderTexture.draw([sprite], -x, -y);
        
        sprite.scaleX = scaleX;
        sprite.scaleY = scaleY;
        sprite.originX = originX;
        sprite.originY = originY;
        sprite.angle = angle;

        const colorArray: Phaser.Display.Color[] = [];

        renderTexture.snapshotPixel(0, 0, (function (colorArray) {
            return function (c: Phaser.Display.Color) {
                consoleLog(c);
                colorArray[0] = c;
            };
        })(colorArray));

        renderTexture.destroy();

        const color = colorArray[0];
        const alpha = color.alpha;

        return alpha > 0;
    }



    function getAlpha_CanvasTexture(hitArea: any, x: number, y: number, sprite: any) {
        if (sprite.flipX) {
            x = 2 * sprite.displayOriginX - x;
        }

        if (sprite.flipY) {
            y = 2 * sprite.displayOriginY - y;
        }

        var alpha = getCanvasTexturePixelAlpha(x, y, sprite.texture);

        return alpha > 0;
    }

    function getCanvasTexturePixelAlpha(x: number, y: number, canvasTexture: Phaser.Textures.CanvasTexture) {
        if (canvasTexture) {
            //if (x >= 0 && x < canvasTexture.width && y >= 0 && y < canvasTexture.height) 
            let imgData = canvasTexture.getContext().getImageData(x, y, 1, 1);
            let rgb = imgData.data;
            let alpha = rgb[3];
            return alpha;
        }
        return 0;
    }

    function getAlpha_SharedTexture(hitArea, x, y, sprite: Phaser.GameObjects.Sprite) {

        if (sprite.flipX) {
            x = 2 * sprite.displayOriginX - x;
        }

        if (sprite.flipY) {
            y = 2 * sprite.displayOriginY - y;
        }

        const textureManager = Editor.getInstance().getGame().textures;
        var alpha = textureManager.getPixelAlpha(x, y, sprite.texture.key, sprite.frame.name);

        return alpha;
    };
}