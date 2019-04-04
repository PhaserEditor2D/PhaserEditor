namespace PhaserEditor2D {

    export class Models {
        static gameConfig = {
            webgl: true
        }
        static displayList = {
            children: []
        };

        static sceneProperties = {

        };

        static selection: any[] = [];
        static projectUrl: string = null;
        static pack: any = {};


        static displayList_updateObjectData(objData: any) {
            var children = this.displayList.children;
            for (var i = 0; i < children.length; i++) {
                // TODO: missing to deal with the containers (objects with children)
                var objData2 = children[i];
                if (objData2["-id"] === objData["-id"]) {
                    children[i] = objData2;
                }
            }
        }

        static updateModel(model: Object, properties: Object) {
            for (let key in properties) {
                model[key] = properties[key];
            }
        }
    }

}