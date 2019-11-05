namespace phasereditor2d.pack.ui.editor.properties {

    import controls = colibri.ui.controls;

    export class AssetPackEditorPropertyProvider extends controls.properties.PropertySectionProvider {

        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void {

            sections.push(new ItemSection(page));
            sections.push(new ImageSection(page));
            sections.push(new SVGSection(page));
            sections.push(new AtlasSection(page));
            sections.push(new AtlasXMLSection(page));
            sections.push(new UnityAtlasSection(page));
            sections.push(new MultiatlasSection(page));
            sections.push(new SpritesheetSection(page));
            sections.push(new BitmapFontSection(page));
            sections.push(new TilemapCSVSection(page));
            sections.push(new TilemapImpactSection(page));
            sections.push(new TilemapTiledJSONSection(page));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.Animations",
                "Animations",
                "URL",
                "url",
                core.contentTypes.CONTENT_TYPE_ANIMATIONS,
                core.ANIMATIONS_TYPE));
            

            sections.push(new ui.properties.ImagePreviewSection(page));
            sections.push(new ui.properties.ManyImageSection(page));
        }

    }
}