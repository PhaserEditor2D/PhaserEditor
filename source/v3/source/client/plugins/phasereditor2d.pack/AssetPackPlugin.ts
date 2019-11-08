/// <reference path="./core/contentTypes/AnimationsContentTypeResolver.ts" />

namespace phasereditor2d.pack {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export const ICON_ASSET_PACK = "asset-pack";
    export const ICON_ANIMATIONS = "animations";

    export class AssetPackPlugin extends ide.Plugin {

        private static _instance = new AssetPackPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.pack");
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // icons loader

            reg.addExtension(
                ide.IconLoaderExtension.POINT_ID,
                ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_ASSET_PACK,
                    ICON_ANIMATIONS
                ])
            );

            // content type resolvers

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.AssetPackContentTypeResolver",
                    [new pack.core.contentTypes.AssetPackContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.AtlasContentTypeResolver",
                    [new pack.core.contentTypes.AtlasContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.MultiatlasContentTypeResolver",
                    [new pack.core.contentTypes.MultiatlasContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.AtlasXMLContentTypeResolver",
                    [new pack.core.contentTypes.AtlasXMLContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.UnityAtlasContentTypeResolver",
                    [new pack.core.contentTypes.UnityAtlasContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.AnimationsContentTypeResolver",
                    [new pack.core.contentTypes.AnimationsContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.BitmapFontContentTypeResolver",
                    [new pack.core.contentTypes.BitmapFontContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.TilemapImpactContentTypeResolver",
                    [new pack.core.contentTypes.TilemapImpactContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.TilemapTiledJSONContentTypeResolver",
                    [new pack.core.contentTypes.TilemapTiledJSONContentTypeResolver()],
                    5
                ));

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.pack.core.contentTypes.AudioSpriteContentTypeResolver",
                    [new pack.core.contentTypes.AudioSpriteContentTypeResolver()],
                    5
                ));

            // content type icons

            reg.addExtension(
                ide.ContentTypeIconExtension.POINT_ID,
                ide.ContentTypeIconExtension.withPluginIcons(this, [
                    {
                        iconName: ICON_ASSET_PACK,
                        contentType: core.contentTypes.CONTENT_TYPE_ASSET_PACK
                    },
                    {
                        iconName: ICON_ANIMATIONS,
                        contentType: core.contentTypes.CONTENT_TYPE_ANIMATIONS
                    },
                    {
                        iconName: files.ICON_FILE_FONT,
                        contentType: core.contentTypes.CONTENT_TYPE_BITMAP_FONT
                    }
                ]));

            // project resources preloader


            reg.addExtension(ide.PreloadProjectResourcesExtension.POINT_ID,
                new ide.PreloadProjectResourcesExtension(
                    "phasereditor2d.pack.PreloadProjectResourcesExtension",
                    () => pack.core.PackFinder.preload()
                )
            );

            // editors

            reg.addExtension(ide.EditorExtension.POINT_ID,
                new ide.EditorExtension("phasereditor2d.pack.EditorExtension", [
                    ui.editor.AssetPackEditor.getFactory()
                ]));

            reg.addExtension(ide.commands.CommandExtension.POINT_ID,
                new ide.commands.CommandExtension("phasereditor2d.scene.commands",
                    ui.editor.AssetPackEditor.registerCommands));

            // new file dialog

            reg.addExtension(phasereditor2d.ide.ui.dialogs.NewFileDialogExtension.POINT,
                new ui.dialogs.NewAssetPackFileWizardExtension());
        }
    }

    ide.Workbench.getWorkbench().addPlugin(AssetPackPlugin.getInstance());
}