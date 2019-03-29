function EditorToolScene() {
    Phaser.Scene.call(this, "toolScene");

    EditorGlobal.toolScene = this;
}

EditorToolScene.prototype = Object.create(Phaser.Scene.prototype);
EditorToolScene.prototype.constructor = EditorToolScene;

EditorToolScene.prototype.create = function () {
    /** @type {Phaser.Scene} */
    var scene = this;

    scene.add.text(10, 10, "Hello tool scene", {
        fill: "#ffffff"
    })

    this._selection = [];
};

EditorToolScene.prototype.updateSelectionObjects = function () {
    
    for(var i = 0; i < this._selection.length; i++) {
        this._selection[i].destroy();
    }

    this._selection = [];

    /** @type {Phaser.Scene} */
    var editorScene = EditorGlobal.editor.getScene();
    /** @type {Phaser.Scene} */
    var self = this;

    var sel = Models.selection;

    for (var i = 0; i < sel.length; i++) {
        var id = sel[i];
        /** @type {Phaser.GameObjects.Image} */
        var obj = editorScene.sys.displayList.getByName(id);
        if (obj) {
            var rect = self.add.rectangle(obj.x, obj.y, obj.displayWidth, obj.displayHeight);
            rect._selectedObject = obj;
            rect.strokeColor = 0x00ff00;
            rect.lineWidth = 1;
            rect.isStroked = true
            this._selection.push(rect);
        }
    }
};

EditorToolScene.prototype.update = function () {

    for (var i = 0; i < this._selection.length; i++) {
        /** @type {Phaser.GameObjects.Rectangle} */
        var rect = this._selection[i];
        this.updateSelectionRect(rect);
    }

};

EditorToolScene.prototype.updateSelectionRect = function (rect) {
    /** @type {Phaser.GameObjects.Sprite} */
    var obj = rect._selectedObject;

    rect.x = obj.x;
    rect.y = obj.y;
    rect.width = obj.width;
    rect.height = obj.height;
};