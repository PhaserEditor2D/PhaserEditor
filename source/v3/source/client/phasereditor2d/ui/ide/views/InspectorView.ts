/// <reference path="../ViewPart.ts"/>
/// <reference path="../../../../phasereditor2d.ui.controls/properties/PropertyPage.ts"/>
/// <reference path="../../../../phasereditor2d.ui.controls/properties/PropertySection.ts"/>
/// <reference path="../../../../phasereditor2d.ui.controls/properties/PropertySectionProvider.ts"/>

namespace phasereditor2d.ui.ide.inspector {

    class MyLayout implements controls.ILayout {
        
        layout(parent: controls.Control) {
            const child = parent.getChildren()[0];
            child.getElement().style.height = parent.getBounds().height + "px";
            child.dispatchLayoutEvent();
        }

    }

    export class InspectorView extends ide.ViewPart {

        private _propertyPage: ui.controls.properties.PropertyPage;
        private _activePart : Part;
        private _selectionListener: any;

        constructor() {
            super("InspectorView");

            this.setTitle("Inspector");

            this._propertyPage = new ui.controls.properties.PropertyPage();

            this.getClientArea().add(this._propertyPage);
            //this.getClientArea().setLayout(new ui.controls.FillLayout());
            this.getClientArea().setLayout(new MyLayout());

            this._selectionListener = (e : CustomEvent) => this.onPartSelection();

            Workbench.getWorkbench().addEventListener(PART_ACTIVATE_EVENT, e => this.onPartActivate());
        }

        private onPartActivate() {
            const part = Workbench.getWorkbench().getActivePart();

            if (!part || part !== this && part !== this._activePart) {
                
                if (this._activePart) {
                    this._activePart.removeEventListener(controls.SELECTION_EVENT, this._selectionListener);
                }

                this._activePart = part;
                
                this._activePart.addEventListener(controls.SELECTION_EVENT, this._selectionListener);

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