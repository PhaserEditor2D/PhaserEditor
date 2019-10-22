namespace phasereditor2d.pack.ui.editor {

    import controls = colibri.ui.controls;

    export class AssetPackEditorPropertySectionProvider extends controls.properties.PropertySectionProvider {

        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void {

            sections.push(new properties.ImageSection(page));

            sections.push(new properties.ManyImageSection(page));
        }

    }

}