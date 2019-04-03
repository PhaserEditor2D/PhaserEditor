namespace PhaserEditor2D {

    export class Models {
        static displayList = {
            children: []
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

        static isReady() {
            return Models.displayList.children.length > 0;
        }
    }

}