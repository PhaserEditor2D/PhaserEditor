var PhaserEditor2D;
(function (PhaserEditor2D) {
    function get_property(name, defaultValue) {
        return function (data) {
            var value = data[name];
            if (value == undefined) {
                return defaultValue;
            }
            return value;
        };
    }
    PhaserEditor2D.TransformComponent = {
        get_x: get_property("x", 0),
        get_y: get_property("y", 0),
        get_scaleX: get_property("scaleX", 1),
        get_scaleY: get_property("scaleY", 1),
        get_angle: get_property("angle", 0),
        updateObject: function (obj, data) {
            obj.x = this.get_x(data);
            obj.y = this.get_y(data);
            obj.scaleX = this.get_scaleX(data);
            obj.scaleY = this.get_scaleY(data);
            obj.angle = this.get_angle(data);
        }
    };
    PhaserEditor2D.OriginComponent = {
        get_originX: get_property("originX", 0.5),
        get_originY: get_property("originY", 0.5),
        updateObject: function (obj, data) {
            obj.setOrigin(this.get_originX(data), this.get_originY(data));
        }
    };
    PhaserEditor2D.TextureComponent = {
        get_textureKey: get_property("textureKey"),
        get_textureFrame: get_property("textureFrame")
    };
    PhaserEditor2D.TileSpriteComponent = {
        get_tilePositionX: get_property("tilePositionX", 0),
        get_tilePositionY: get_property("tilePositionY", 0),
        get_tileScaleX: get_property("tileScaleX", 1),
        get_tileScaleY: get_property("tileScaleY", 1),
        get_width: get_property("width", -1),
        get_height: get_property("height", -1),
        updateObject: function (obj, data) {
            obj.setTilePosition(this.get_tilePositionX(data), this.get_tilePositionY(data));
            obj.setTileScale(this.get_tileScaleX(data), this.get_tileScaleY(data));
        }
    };
    PhaserEditor2D.FlipComponent = {
        get_flipX: get_property("flipX", false),
        get_flipY: get_property("flipY", false),
        updateObject: function (obj, data) {
            obj.flipX = this.get_flipX(data);
            obj.flipY = this.get_flipY(data);
        }
    };
    PhaserEditor2D.BitmapTextComponent = {
        get_fontSize: get_property("fontSize", 0),
        get_align: get_property("align", 0),
        get_letterSpacing: get_property("letterSpacing", 0),
        get_fontAssetKey: get_property("fontAssetKey"),
        get_originX: get_property("originX", 0),
        get_originY: get_property("originY", 0),
        updateObject: function (obj, data) {
            obj.text = PhaserEditor2D.TextualComponent.get_text(data);
            obj.fontSize = PhaserEditor2D.BitmapTextComponent.get_fontSize(data);
            obj.align = PhaserEditor2D.BitmapTextComponent.get_align(data);
            obj.setOrigin(this.get_originX(data), this.get_originY(data));
        }
    };
    PhaserEditor2D.TextualComponent = {
        get_text: get_property("text", "")
    };
})(PhaserEditor2D || (PhaserEditor2D = {}));
