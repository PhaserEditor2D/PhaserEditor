namespace phasereditor2d.ui.parts {    

    export abstract class Part extends ui.controls.Panel {
        private _id: string;

        constructor(id: string) {
            super();
            this._id = id;
            this.getElement().classList.add("part");
        }

        getId() {
            return this._id;
        }
    }

    export class ViewPart extends Part {

        constructor(id: string) {
            super(id);
            this.getElement().classList.add("view");           
        }
    }

    export class EditorArea extends Part {
        constructor() {
            super("editorArea");
        }
    }
}