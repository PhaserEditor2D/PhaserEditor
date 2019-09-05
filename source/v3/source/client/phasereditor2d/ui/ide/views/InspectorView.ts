/// <reference path="../ViewPart.ts"/>
/// <reference path="../../../../phasereditor2d.ui.controls/properties/PropertyPage.ts"/>
/// <reference path="../../../../phasereditor2d.ui.controls/properties/PropertySection.ts"/>
/// <reference path="../../../../phasereditor2d.ui.controls/properties/IPropertySectionProvider.ts"/>

namespace phasereditor2d.ui.ide.inspector {



    class Sample1Section extends controls.properties.PropertySection<any> {

        protected createForm(parent: HTMLDivElement) {
            parent.innerHTML = "<label>Sample Section 1</label>";
        } 
        
        canEdit(obj: any) {
            return true;
        }

        canEditNumber(n: number) {
            return true;
        }
    }

    class Sample2Section extends controls.properties.PropertySection<any> {

        protected createForm(parent: HTMLDivElement) {
            parent.innerHTML = "<label>Sample Section 2</label>";
        } 
        
        canEdit(obj: any) {
            return true;
        }

        canEditNumber(n: number) {
            return true;
        }
    }

    class SampleSectionProvider implements controls.properties.IPropertySectionProvider {

        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void {
            sections.push(new Sample1Section(page, "sample1", "Sample 1"));
            sections.push(new Sample2Section(page, "sample2", "Sample 2"));
        }
    }

    export class InspectorView extends ide.ViewPart {

        private _propertyPage: ui.controls.properties.PropertyPage;

        constructor() {
            super("inspectorView");

            this.setTitle("Inspector");

            this._propertyPage = new ui.controls.properties.PropertyPage();
            this._propertyPage.setSectionProvider(new SampleSectionProvider());

            this.getClientArea().add(this._propertyPage);
            this.getClientArea().setLayout(new ui.controls.FillLayout());
        }
    }
}