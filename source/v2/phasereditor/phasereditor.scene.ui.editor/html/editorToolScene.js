function EditorToolScene() {
    Phaser.Scene.call(this, "toolScene");

    EditorGlobal.toolScene = this;

    this._selection = [];
}

EditorToolScene.prototype = Object.create(Phaser.Scene.prototype);
EditorToolScene.prototype.constructor = EditorToolScene;

EditorToolScene.prototype.create = function () {
};

EditorToolScene.prototype.updateSelectionObjects = function () {

    for (var i = 0; i < this._selection.length; i++) {
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
            var rect = self.add.rectangle(obj.x, obj.y, obj.displayWidth, obj.displayHeight, 0x00ff00, 0.1);
            rect._selectedObject = obj;
            rect.strokeColor = 0x00ff00;
            rect.fillColor = rect.strokeColor;
            rect.isStroked = true
            this._selection.push(rect);
        }
    }
};

EditorToolScene.prototype.update = function () {
    this.syncCamera();

    for (var i = 0; i < this._selection.length; i++) {
        /** @type {Phaser.GameObjects.Rectangle} */
        var rect = this._selection[i];
        this.updateSelectionRect(rect);
    }

};

EditorToolScene.prototype.syncCamera = function () {
    /** @type {Phaser.Scene} */
    var editorScene = EditorGlobal.editor.getScene();
    /** @type {Phaser.Scene} */
    var self = this;

    var editorCamera = editorScene.cameras.main;
    var camera = self.cameras.main;

    camera.zoom = editorCamera.zoom;
    camera.setScroll(editorCamera.scrollX, editorCamera.scrollY);
};

/**
 * @param {Phaser.Scene.GameObjects.Rectangle} rect
 */
EditorToolScene.prototype.updateSelectionRect = function (rect) {
    /** @type {Phaser.Scene} */
    var self = EditorGlobal.editor.getScene();

    /** @type {Phaser.GameObjects.Sprite} */
    var obj = rect._selectedObject;

    rect.lineWidth = 1 / self.cameras.main.zoom;
    rect.x = obj.x;
    rect.y = obj.y;
    rect.displayWidth = obj.displayWidth;
    rect.displayHeight = obj.displayHeight;
    rect.angle = obj.angle;
    rect.originX = obj.originX;
    rect.originY = obj.originY;
};