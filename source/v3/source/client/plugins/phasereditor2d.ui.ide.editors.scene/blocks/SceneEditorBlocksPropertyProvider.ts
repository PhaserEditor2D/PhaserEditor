
namespace phasereditor2d.ui.ide.editors.scene.blocks {

    import controls = colibri.ui.controls;

    export class SceneEditorBlocksPropertyProvider extends controls.properties.PropertySectionProvider {

        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void {

            sections.push(new pack.properties.AssetPackItemSection(page));
            sections.push(new pack.properties.ImageSection(page));
            sections.push(new pack.properties.ManyImageSection(page));

        }

    }

}