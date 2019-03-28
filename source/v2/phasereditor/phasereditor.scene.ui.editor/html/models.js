var Models = {
    displayList: undefined,
    displayList_updateObjectData: function (objData) {
        var children = this.displayList.children;
        for(var i = 0; i < children.length; i++) {
            // TODO: missing to deal with the containers (objects with children)
            var objData2 = children[i];
            if (objData2["-id"] === objData["-id"]) {
                children[i] = objData2;
            }
        }
    },
    packs: [],
    projectUrl: ""
}