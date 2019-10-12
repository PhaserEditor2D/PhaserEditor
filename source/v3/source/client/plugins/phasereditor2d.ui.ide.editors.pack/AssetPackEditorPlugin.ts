namespace phasereditor2d.ui.ide.editors.pack {

    export const ICON_ASSET_PACK = "asset-pack";

    export class AssetPackEditorPlugin extends Plugin {

        private static _instance = new AssetPackEditorPlugin();

        static getInstance(): Plugin {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ui.ide.editors.pack.AssetPackEditorPlugin");
        }

        registerContentTypes(registry: core.ContentTypeRegistry): void {
            registry.registerResolver(new editors.pack.AssetPackContentTypeResolver());
        }

        async preloadProjectResources() {
            await editors.pack.PackFinder.preload();
        }

        async preloadIcons(contentTypeIconMap: Map<string, controls.IImage>): Promise<void> {

            await this.getIcon(ICON_ASSET_PACK).preload();

            contentTypeIconMap.set(CONTENT_TYPE_ASSET_PACK, this.getIcon(ICON_ASSET_PACK));
        }

        registerEditor(registry: EditorRegistry) {
            registry.registerFactory(editors.pack.AssetPackEditor.getFactory());
        }

        getIcon(icon: string) {
            return controls.Controls.getIcon(icon, "plugins/phasereditor2d.ui.ide.editors.pack/icons");
        }

    }

}