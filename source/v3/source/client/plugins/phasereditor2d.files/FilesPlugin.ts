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
            super("phasereditor2d.files.FilesPlugin");
        }

        registerContentTypes(registry: colibri.core.ContentTypeRegistry) {

            registry.registerResolver(new core.DefaultExtensionTypeResolver());

        }

        registerContentTypeIcons(contentTypeIconMap: Map<string, controls.IImage>) {

            contentTypeIconMap.set(core.CONTENT_TYPE_IMAGE, this.getIcon(ICON_FILE_IMAGE));
            contentTypeIconMap.set(core.CONTENT_TYPE_AUDIO, this.getIcon(ICON_FILE_SOUND));
            contentTypeIconMap.set(core.CONTENT_TYPE_VIDEO, this.getIcon(ICON_FILE_VIDEO));
            contentTypeIconMap.set(core.CONTENT_TYPE_SCRIPT, this.getIcon(ICON_FILE_SCRIPT));
            contentTypeIconMap.set(core.CONTENT_TYPE_TEXT, this.getIcon(ICON_FILE_TEXT));

        }

        getIcon(name: string) {
            return controls.Controls.getIcon(name, "plugins/phasereditor2d.files/ui/icons");
        }

    }

}