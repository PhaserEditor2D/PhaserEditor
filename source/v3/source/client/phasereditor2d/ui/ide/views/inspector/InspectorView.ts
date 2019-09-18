/// <reference path="../../ViewPart.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertyPage.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySection.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySectionProvider.ts"/>

namespace phasereditor2d.ui.ide.inspector {

    export class InspectorView extends ide.ViewPart {

        private _propertyPage: ui.controls.properties.PropertyPage;
        private _activePart : Part;
        private _selectionListener: any;

        constructor() {
            super("InspectorView");

            this.setTitle("Inspector");
            this.setIcon(Workbench.getWorkbench().getWorkbenchIcon(ICON_INSPECTOR));
        }

        layout() {
            this._propertyPage.dispatchLayoutEvent();
        }

        createPart() {
            super.createPart();

            this._propertyPage = new ui.controls.properties.PropertyPage();

            this.add(this._propertyPage);

            this._selectionListener = (e : CustomEvent) => this.onPartSelection();

            Workbench.getWorkbench().addEventListener(EVENT_PART_ACTIVATE, e => this.onWorkbenchPartActivate());
        }

        private onWorkbenchPartActivate() {
            const part = Workbench.getWorkbench().getActivePart();

            if (!part || part !== this && part !== this._activePart) {
                
                if (this._activePart) {
                    this._activePart.removeEventListener(controls.EVENT_SELECTION, this._selectionListener);
                }

                this._activePart = part;
                
                this._activePart.addEventListener(controls.EVENT_SELECTION, this._selectionListener);

                this.onPartSelection();
            }
        }

        private onPartSelection() {
            const sel = this._activePart.getSelection();
            const provider = this._activePart.getPropertyProvider();
            this._propertyPage.setSectionProvider(provider);
            this._propertyPage.setSelection(sel);
        }
    }
}