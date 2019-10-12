
namespace phasereditor2d.ui.ide.views.inspector {

    export class InspectorView extends ide.ViewPart {

        private _propertyPage: ui.controls.properties.PropertyPage;
        private _currentPart: Part;
        private _selectionListener: any;

        constructor() {
            super("InspectorView");

            this.setTitle("Inspector");
            this.setIcon(Workbench.getWorkbench().getWorkbenchIcon(ICON_INSPECTOR));
        }

        layout() {
            this._propertyPage.dispatchLayoutEvent();
        }

        protected createPart() {
            this._propertyPage = new ui.controls.properties.PropertyPage();

            this.add(this._propertyPage);

            this._selectionListener = (e: CustomEvent) => this.onPartSelection();

            Workbench.getWorkbench().addEventListener(EVENT_PART_ACTIVATED, e => this.onWorkbenchPartActivate());
        }

        private onWorkbenchPartActivate() {
            const part = Workbench.getWorkbench().getActivePart();

            if (part !== this && part !== this._currentPart) {

                if (this._currentPart) {
                    this._currentPart.removeEventListener(controls.EVENT_SELECTION_CHANGED, this._selectionListener);
                }

                this._currentPart = part;

                if (part) {
                    part.addEventListener(controls.EVENT_SELECTION_CHANGED, this._selectionListener);
                    this.onPartSelection();
                } else {
                    this._propertyPage.setSectionProvider(null);
                }
            }
        }

        private onPartSelection() {
            const sel = this._currentPart.getSelection();
            const provider = this._currentPart.getPropertyProvider();
            this._propertyPage.setSectionProvider(provider);
            this._propertyPage.setSelection(sel);
        }
    }
}