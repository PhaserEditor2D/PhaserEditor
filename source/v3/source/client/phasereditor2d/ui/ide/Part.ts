/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/Panel.ts"/>

namespace phasereditor2d.ui.ide {

    export abstract class Part extends ui.controls.Panel {
        private _id: string;

        constructor(id: string) {
            super();
            this._id = id;
            this.getElement().classList.add("Part");
            (<any>this.getElement()).__part = this;
        }

        getId() {
            return this._id;
        }
    }
}