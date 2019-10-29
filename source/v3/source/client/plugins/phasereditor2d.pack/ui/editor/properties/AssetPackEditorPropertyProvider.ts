namespace phasereditor2d.pack.ui.editor.properties {

    import controls = colibri.ui.controls;

    export class AssetPackEditorPropertyProvider extends controls.properties.PropertySectionProvider {

        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void {

            sections.push(new ItemSection(page));
            sections.push(new ImageSection(page));

            sections.push(new ui.properties.ImagePreviewSection(page));
            sections.push(new ui.properties.ManyImageSection(page));
        }

    }
}