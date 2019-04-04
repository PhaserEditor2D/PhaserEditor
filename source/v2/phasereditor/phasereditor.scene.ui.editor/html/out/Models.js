var PhaserEditor2D;
(function (PhaserEditor2D) {
    var Models = (function () {
        function Models() {
        }
        Models.displayList_updateObjectData = function (objData) {
            var children = this.displayList.children;
            for (var i = 0; i < children.length; i++) {
                var objData2 = children[i];
                if (objData2["-id"] === objData["-id"]) {
                    children[i] = objData2;
                }
            }
        };
        Models.updateModel = function (model, properties) {
            for (var key in properties) {
                model[key] = properties[key];
            }
        };
        Models.gameConfig = {
            webgl: true
        };
        Models.displayList = {
            children: []
        };
        Models.sceneProperties = {};
        Models.selection = [];
        Models.projectUrl = null;
        Models.pack = {};
        return Models;
    }());
    PhaserEditor2D.Models = Models;
})(PhaserEditor2D || (PhaserEditor2D = {}));
