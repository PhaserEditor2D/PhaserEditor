var PhaserEditor2D;
(function (PhaserEditor2D) {
    var BuildMessage = (function () {
        function BuildMessage() {
        }
        BuildMessage.SetTileSpriteProperties = function (objects) {
            var list = [];
            for (var _i = 0, objects_1 = objects; _i < objects_1.length; _i++) {
                var obj = objects_1[_i];
                var sprite = obj;
                var data = { id: sprite.name };
                PhaserEditor2D.TileSpriteComponent.updateData(sprite, data);
                list.push(data);
            }
            return {
                method: "SetTileSpriteProperties",
                list: list
            };
        };
        BuildMessage.SetOriginProperties = function (objects) {
            var list = [];
            for (var _i = 0, objects_2 = objects; _i < objects_2.length; _i++) {
                var obj = objects_2[_i];
                var data = { id: obj.name };
                PhaserEditor2D.OriginComponent.updateData(obj, data);
                PhaserEditor2D.TransformComponent.set_x(data, obj.x);
                PhaserEditor2D.TransformComponent.set_y(data, obj.y);
                list.push(data);
            }
            return {
                method: "SetOriginProperties",
                list: list
            };
        };
        return BuildMessage;
    }());
    PhaserEditor2D.BuildMessage = BuildMessage;
})(PhaserEditor2D || (PhaserEditor2D = {}));
