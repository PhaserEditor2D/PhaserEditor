/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/Panel.ts"/>

namespace phasereditor2d.ui.ide {

    export abstract class Part extends controls.Control {
        private _id: string;
        private _title: string;
        private _selection: any[];

        constructor(id: string) {
            super();
            
            this._id = id;
            this._title = "";
            this._selection = [];
            
            this.getElement().setAttribute("id", id);

            this.getElement().classList.add("Part");
            
            this.getElement()["__part"] = this;
        }

        getTitle() {
            return this._title;
        }

        setTitle(title : string) : void {
            this._title = title;
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

        layout() {
            
        }
    }
}