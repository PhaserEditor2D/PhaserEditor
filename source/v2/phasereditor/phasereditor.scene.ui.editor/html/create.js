var EditorCreate = {

    /**
     * @param {Phaser.GameObjects.GameObjectFactory} add 
     */
    createWorld: function (add) {
        if (!Models.displayList) {
            return;
        }
        
        var list = Models.displayList.children;

        for (var i = 0; i < list.length; i++) {
            var data = list[i];
            EditorCreate.createObject(add, data);
        }
    },

    /**
     * 
     * @param {Phaser.GameObjects.GameObjectFactory} add 
     * @param {Object} data 
     */
    createObject: function (add, data) {
        console.log("Create object");
        console.log(data);

        var type = data["-type"];

        switch (type) {
            case "Image":
                var x = TransformComponent.get_x(data);
                var y = TransformComponent.get_y(data);
                var key = TextureComponent.get_textureKey(data);
                var frame = TextureComponent.get_textureFrame(data);

                var obj = add.image(x, y, key, frame);

                TransformComponent.updateObject(obj, data);
                OriginComponent.updateObject(obj, data);

                break;
            case "Sprite":

                var x = TransformComponent.get_x(data);
                var y = TransformComponent.get_y(data);
                var key = TextureComponent.get_textureKey(data);
                var frame = TextureComponent.get_textureFrame(data);

                var obj = add.sprite(x, y, key, frame);

                TransformComponent.updateObject(obj, data);
                OriginComponent.updateObject(obj, data);

                break;
        }
    }

};