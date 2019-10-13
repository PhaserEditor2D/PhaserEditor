namespace phasereditor2d.pack {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export const ICON_ASSET_PACK = "asset-pack";

    export class AssetPackPlugin extends ide.Plugin {

        private static _instance = new AssetPackPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.pack");
        }

        registerContentTypes(registry: colibri.core.ContentTypeRegistry): void {
            registry.registerResolver(new pack.core.AssetPackContentTypeResolver());
        }

        async preloadProjectResources() {
            await pack.core.PackFinder.preload();
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            reg.addExtension(
                ide.IconLoaderExtension.POINT_ID,
                ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_ASSET_PACK
                ])
            );

        }

        async registerContentTypeIcons(contentTypeIconMap: Map<string, controls.IImage>): Promise<void> {
            contentTypeIconMap.set(pack.core.CONTENT_TYPE_ASSET_PACK, this.getIcon(ICON_ASSET_PACK));
        }

        registerEditor(registry: ide.EditorRegistry) {
            registry.registerFactory(ui.editor.AssetPackEditor.getFactory());
        }

    }

}