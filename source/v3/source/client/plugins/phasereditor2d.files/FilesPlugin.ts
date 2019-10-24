namespace phasereditor2d.files {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export const ICON_FILE_FONT = "file-font";
    export const ICON_FILE_IMAGE = "file-image";
    export const ICON_FILE_VIDEO = "file-movie";
    export const ICON_FILE_SCRIPT = "file-script";
    export const ICON_FILE_SOUND = "file-sound";
    export const ICON_FILE_TEXT = "file-text";

    export class FilesPlugin extends ide.Plugin {

        private static _instance = new FilesPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.files");
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // icons loader

            reg.addExtension(
                colibri.ui.ide.IconLoaderExtension.POINT_ID,
                colibri.ui.ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_FILE_IMAGE,
                    ICON_FILE_SOUND,
                    ICON_FILE_VIDEO,
                    ICON_FILE_SCRIPT,
                    ICON_FILE_TEXT,
                    ICON_FILE_FONT
                ])
            );

            // content type resolvers

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.files.core.DefaultExtensionTypeResolver",
                    [new core.DefaultExtensionTypeResolver()],
                    1000
                ));

            // content type icons

            reg.addExtension(
                ide.ContentTypeIconExtension.POINT_ID,
                ide.ContentTypeIconExtension.withPluginIcons(this, [
                    {
                        iconName: ICON_FILE_IMAGE,
                        contentType: core.CONTENT_TYPE_IMAGE
                    },
                    {
                        iconName: ICON_FILE_IMAGE,
                        contentType: core.CONTENT_TYPE_SVG
                    },
                    {
                        iconName: ICON_FILE_SOUND,
                        contentType: core.CONTENT_TYPE_AUDIO
                    },
                    {
                        iconName: ICON_FILE_VIDEO,
                        contentType: core.CONTENT_TYPE_VIDEO
                    },
                    {
                        iconName: ICON_FILE_SCRIPT,
                        contentType: core.CONTENT_TYPE_SCRIPT
                    },
                    {
                        iconName: ICON_FILE_SCRIPT,
                        contentType: core.CONTENT_TYPE_JAVASCRIPT
                    },
                    {
                        iconName: ICON_FILE_SCRIPT,
                        contentType: core.CONTENT_TYPE_CSS
                    },
                    {
                        iconName: ICON_FILE_SCRIPT,
                        contentType: core.CONTENT_TYPE_HTML
                    },
                    {
                        iconName: ICON_FILE_TEXT,
                        contentType: core.CONTENT_TYPE_TEXT
                    }
                ]));
        }
    }

    ide.Workbench.getWorkbench().addPlugin(FilesPlugin.getInstance());
}