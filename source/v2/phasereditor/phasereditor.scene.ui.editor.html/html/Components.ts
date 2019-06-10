namespace PhaserEditor2D {

    function get_property(name: string, defaultValue?: any) {
        return function (data: any) {
            var value = data[name];
            if (value == undefined) {
                return defaultValue;
            }
            return value;
        };
    }

    function set_property(name: string) {
        return function (data: any, value: any) {
            data[name] = value;
        };
    }

    export const GameObjectEditorComponent = {
        get_gameObjectEditorTransparency: get_property("gameObjectEditorTransparency", 1),

        updateObject: function (obj: Phaser.GameObjects.Components.Alpha, data: any) {
            obj.alpha *= this.get_gameObjectEditorTransparency(data);
        }
    };


    export const TransformComponent = {
        get_x: get_property("x", 0),
        set_x: set_property("x"),

        get_y: get_property("y", 0),
        set_y: set_property("y"),

        get_scaleX: get_property("scaleX", 1),
        set_scaleX: set_property("scaleX"),

        get_scaleY: get_property("scaleY", 1),
        set_scaleY: set_property("scaleY"),

        get_angle: get_property("angle", 0),
        set_angle: set_property("angle"),

        updateObject: function (obj: Phaser.GameObjects.Components.Transform, data: any) {
            obj.x = this.get_x(data);
            obj.y = this.get_y(data);
            obj.scaleX = this.get_scaleX(data);
            obj.scaleY = this.get_scaleY(data);
            obj.angle = this.get_angle(data);
        },

        updateData: function (obj: Phaser.GameObjects.Components.Transform, data: any) {
            this.set_x(data, obj.x);
            this.set_y(data, obj.y);
            this.set_scaleX(data, obj.scaleX);
            this.set_scaleY(data, obj.scaleY);
            this.set_angle(data, obj.angle);
        }
    };

    export const OriginComponent = {
        get_originX: get_property("originX", 0.5),
        set_originX: set_property("originX"),
        get_originY: get_property("originY", 0.5),
        set_originY: set_property("originY"),

        updateObject: function (obj: Phaser.GameObjects.Components.Origin, data: any) {
            obj.setOrigin(data.originX || 0.5, data.originY || 0.5);
        },

        updateData: function (obj: Phaser.GameObjects.Components.Origin, data: any) {
            this.set_originX(data, obj.originX);
            this.set_originY(data, obj.originY);
        }
    };

    export const TextureComponent = {
        get_textureKey: get_property("textureKey"),
        get_textureFrame: get_property("textureFrame")
    };

    export const TileSpriteComponent = {
        get_tilePositionX: get_property("tilePositionX", 0),
        set_tilePositionX: set_property("tilePositionX"),
        get_tilePositionY: get_property("tilePositionY", 0),
        set_tilePositionY: set_property("tilePositionY"),

        get_tileScaleX: get_property("tileScaleX", 1),
        set_tileScaleX: set_property("tileScaleX"),
        get_tileScaleY: get_property("tileScaleY", 1),
        set_tileScaleY: set_property("tileScaleY"),

        get_width: get_property("width", -1),
        set_width: set_property("width"),

        get_height: get_property("height", -1),
        set_height: set_property("height"),

        updateObject: function (obj: Phaser.GameObjects.TileSprite, data: any) {
            obj.setTilePosition(this.get_tilePositionX(data), this.get_tilePositionY(data));
            obj.setTileScale(this.get_tileScaleX(data), this.get_tileScaleY(data));
            obj.width = this.get_width(data);
            obj.height = this.get_height(data);
        },

        updateData: function (obj: Phaser.GameObjects.TileSprite, data: any) {
            this.set_tilePositionX(data, obj.tilePositionX);
            this.set_tilePositionY(data, obj.tilePositionY);

            this.set_tileScaleX(data, obj.tileScaleX);
            this.set_tileScaleY(data, obj.tileScaleY);

            this.set_width(data, obj.width);
            this.set_height(data, obj.height);
        }
    };

    export const FlipComponent = {
        get_flipX: get_property("flipX", false),
        get_flipY: get_property("flipY", false),

        updateObject: function (obj: Phaser.GameObjects.Components.Flip, data: any) {
            obj.flipX = this.get_flipX(data);
            obj.flipY = this.get_flipY(data);
        }
    };

    export const BitmapTextComponent = {
        get_fontSize: get_property("fontSize", 0),
        get_align: get_property("align", 0),
        get_letterSpacing: get_property("letterSpacing", 0),
        get_fontAssetKey: get_property("fontAssetKey"),

        // the BitmapText object has a default origin of 0, 0
        get_originX: get_property("originX", 0),
        get_originY: get_property("originY", 0),

        updateObject: function (obj: Phaser.GameObjects.BitmapText, data: any) {
            obj.text = TextualComponent.get_text(data);
            obj.fontSize = this.get_fontSize(data);
            obj.align = this.get_align(data);
            obj.letterSpacing = this.get_letterSpacing(data);
            obj.setOrigin(this.get_originX(data), this.get_originY(data));
        }
    };

    export const DynamicBitmapTextComponent = {
        get_cropWidth: get_property("cropWidth", 0),
        get_cropHeight: get_property("cropHeight", 0),
        get_scrollX: get_property("scrollX", 0),
        get_scrollY: get_property("scrollY", 0),

        updateObject: function (obj: Phaser.GameObjects.DynamicBitmapText, data: any) {
            obj.cropWidth = this.get_cropWidth(data);
            obj.cropHeight = this.get_cropHeight(data);
            obj.scrollX = this.get_scrollX(data);
            obj.scrollY = this.get_scrollY(data);
        }
    };

    export const TextualComponent = {
        get_text: get_property("text", ""),

        updateObject: function (obj: any, data: any) {
            obj.text = data.text;
        }
    };

    export const TextComponent = {

        updateObject: function (obj: Phaser.GameObjects.Text, data: any) {
            obj.style.align = data.align || "left";
            obj.style.fontFamily = data.fontFamily || "Courier";
            obj.style.fontSize = data.fontSize || "16px";
            obj.style.fontStyle = data.fontStyle || "normal";
            obj.style.backgroundColor = data.backgroundColor || null;
            obj.style.color = data.color || "#fff";
            obj.style.stroke = data.stroke || "#fff";
            obj.style.strokeThickness = data.strokeThickness || 0;
            
            obj.style.maxLines = data.maxLines || 0;
            obj.style.fixedWidth = data.fixedWidth || 0;
            obj.style.fixedHeight = data.fixedHeight || 0;
            obj.style.baselineX = data.baselineX || 1.2;
            obj.style.baselineY = data.baselineY || 1.4;

            obj.style.shadowOffsetX = data["shadow.offsetX"] || 0;
            obj.style.shadowOffsetY = data["shadow.offsetY"] || 0;
            obj.style.shadowColor = data["shadow.color"] || "#000";
            obj.style.shadowBlur = data["shadow.blur"] || 0;
            obj.style.shadowStroke = data["shadow.stroke"] || false;
            obj.style.shadowFill = data["shadow.fill"] || false; 

            obj.style.setWordWrapWidth(data["wordWrap.width"] || 0, data["wordWrap.useAdvancedWrap"] || false);

            obj.style.update(true);

            obj.setLineSpacing(data.lineSpacing || 0);
            obj.setPadding(data.paddingLeft, data.paddingTop, data.paddingRight, data.paddingBottom);  
            
            // Text object has default origin at 0,0
            obj.setOrigin(data.originX || 0, data.originY || 0);
        }
    };


    export const VisibleComponent = {
        get_visible: get_property("visible", true),

        updateObject: function (obj: any, data: any) {
            obj.alpha = this.get_visible(data) ? 1 : 0.5;
        }
    };

    export const ScenePropertiesComponent = {
        get_snapEnabled: get_property("snapEnabled", false),
        get_snapWidth: get_property("snapWidth", 16),
        get_snapHeight: get_property("snapHeight", 16),
        get_backgroundColor: get_property("backgroundColor", "192,192,182"),
        get_foregroundColor: get_property("foregroundColor", "255,255,255"),
        get_borderX: get_property("borderX", 0),
        get_borderY: get_property("borderY", 0),
        get_borderWidth: get_property("borderWidth", 800),
        get_borderHeight: get_property("borderHeight", 600),
    };

    export const TintComponent = {
        get_isTinted: get_property("isTinted", false),
        get_tintFill: get_property("tintFill", false),
        get_tintTopLeft: get_property("tintTopLeft", 0xffffff),
        get_tintTopRight: get_property("tintTopRight", 0xffffff),
        get_tintBottomLeft: get_property("tintBottomLeft", 0xffffff),
        get_tintBottomRight: get_property("tintBottomRight", 0xffffff),

        updateObject: function (obj: Phaser.GameObjects.Components.Tint, data: any) {
            if (this.get_isTinted(data)) {
                if (this.get_tintFill(data)) {
                    obj.setTintFill(
                        this.get_tintTopLeft(data),
                        this.get_tintTopRight(data),
                        this.get_tintBottomLeft(data),
                        this.get_tintBottomRight(data)
                    );
                } else {
                    obj.setTint(
                        this.get_tintTopLeft(data),
                        this.get_tintTopRight(data),
                        this.get_tintBottomLeft(data),
                        this.get_tintBottomRight(data)
                    );
                }
            } else {
                obj.clearTint();
            }
        }
    };
}