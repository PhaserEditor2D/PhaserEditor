/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/Panel.ts"/>

namespace phasereditor2d.ui.ide {

    export abstract class Part extends ui.controls.Panel {
        private _id: string;
        private _selection: any[];

        constructor(id: string) {
            super();
            
            this._id = id;
            this._selection = [];


            this.getElement().classList.add("Part");
            (<any>this.getElement()).__part = this;
        }

        getId() {
            return this._id;
        }

        setSelection(selection: any[]) {
            this._selection = selection;
            this.dispatchEvent(new CustomEvent(controls.SELECTION_EVENT, {
                detail: selection
            }));
        }

        getSelection() {
            return this._selection;
        }

        getPropertyProvider() : controls.properties.PropertySectionProvider {
            return null;
        }
    }
}