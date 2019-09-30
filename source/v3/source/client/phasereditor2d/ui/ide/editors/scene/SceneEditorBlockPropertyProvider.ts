/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySectionProvider.ts" />

namespace phasereditor2d.ui.ide.editors.scene {

    export class SceneEditorBlockPropertyProvider extends controls.properties.PropertySectionProvider {

        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void {
            sections.push(new pack.properties.AssetPackItemSection(page));
            sections.push(new pack.properties.ImageSection(page));
            sections.push(new pack.properties.ManyImageFrameSection(page));
        }

    }

}