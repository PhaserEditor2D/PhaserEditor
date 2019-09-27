/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>

namespace phasereditor2d.ui.ide {

    export const EVENT_PART_TITLE_UPDATED = "partTitledUpdated";

    export abstract class Part extends controls.Control {
        private _id: string;
        private _title: string;
        private _selection: any[];
        private _partCreated: boolean;
        private _icon: controls.IImage;

        constructor(id: string) {
            super();

            this._id = id;
            this._title = "";
            this._selection = [];
            this._partCreated = false;

            this.getElement().setAttribute("id", id);

            this.getElement().classList.add("Part");

            this.getElement()["__part"] = this;
        }

        getTitle() {
            return this._title;
        }

        setTitle(title: string): void {
            this._title = title;
            this.dispatchTitleUpdatedEvent();
        }

        setIcon(icon: controls.IImage) {
            this._icon = icon;
            this.dispatchTitleUpdatedEvent();
        }

        protected dispatchTitleUpdatedEvent() {
            this.dispatchEvent(new CustomEvent(EVENT_PART_TITLE_UPDATED, { detail: this }));   
        }

        getIcon() {
            return this._icon;
        }

        getId() {
            return this._id;
        }

        setSelection(selection: any[]): void {
            this._selection = selection;
            this.dispatchEvent(new CustomEvent(controls.EVENT_SELECTION, {
                detail: selection
            }));
        }

        getSelection() {
            return this._selection;
        }

        getPropertyProvider(): controls.properties.PropertySectionProvider {
            return null;
        }

        layout(): void {

        }

        onPartClosed(): void {

        }

        onPartShown(): void {
            if (!this._partCreated) {
                this._partCreated = true;
                this.createPart();
            }
        }

        protected abstract createPart(): void;
    }
}