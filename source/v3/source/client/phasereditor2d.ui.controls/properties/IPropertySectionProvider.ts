namespace phasereditor2d.ui.controls.properties {
    export interface IPropertySectionProvider {
        addSections(page : PropertyPage, sections : PropertySection<any>[]) : void;
    }
}