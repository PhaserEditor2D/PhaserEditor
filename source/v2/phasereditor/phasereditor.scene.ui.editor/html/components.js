function get_property(name, defaultValue) {
    return function (data) {
        var value = data[name];
        if (value == undefined) {
            return defaultValue;
        }
        return value;
    };
}

var TransformComponent = {
    get_x: get_property("x", 0),
    get_y: get_property("y", 0),
    get_scaleX: get_property("scaleX", 1),
    get_scaleY: get_property("scaleY", 1),
    get_angle: get_property("angle", 0),

    updateObject: function (obj, data) {
        obj.scaleX = this.get_scaleX(data);
        obj.scaleY = this.get_scaleY(data);
        obj.angle = this.get_angle(data);
    }
};

var OriginComponent = {
    get_originX: get_property("originX", 0.5),
    get_originY: get_property("originY", 0.5),

    updateObject: function (obj, data) {
        obj.setOrigin(this.get_originX(data), this.get_originY(data));
    }
};

var TextureComponent = {
    get_textureKey: get_property("textureKey"),
    get_textureFrame: get_property("textureFrame")
};