/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySection.ts" />
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySectionProvider.ts" />

namespace phasereditor2d.ui.ide.views.files {

    export class FilePropertySectionProvider extends controls.properties.PropertySectionProvider {

        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void {
            sections.push(new FileSection(page));
            sections.push(new ImageFileSection(page));
            sections.push(new ManyImageFileSection(page));
        }

    }
}