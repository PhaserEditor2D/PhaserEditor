namespace colibri.ui.ide {
    export const CONTENT_TYPE_IMAGE = "image";
    export const CONTENT_TYPE_AUDIO = "audio";
    export const CONTENT_TYPE_VIDEO = "video";
    export const CONTENT_TYPE_SCRIPT = "script";
    export const CONTENT_TYPE_TEXT = "text";


    export class ExtensionContentTypeResolver implements core.IContentTypeResolver {
        private _map: Map<string, string>;

        constructor(defs: string[][]) {
            this._map = new Map();
            for (const def of defs) {
                this._map.set(def[0].toUpperCase(), def[1]);
            }
        }

        computeContentType(file: core.io.FilePath): Promise<string> {
            const ext = file.getExtension().toUpperCase();
            if (this._map.has(ext)) {
                return Promise.resolve(this._map.get(ext));
            }
            return Promise.resolve(core.CONTENT_TYPE_ANY);
        }

    }

    export class DefaultExtensionTypeResolver extends ExtensionContentTypeResolver {
        constructor() {
            super([
                ["png", CONTENT_TYPE_IMAGE],
                ["jpg", CONTENT_TYPE_IMAGE],
                ["bmp", CONTENT_TYPE_IMAGE],
                ["gif", CONTENT_TYPE_IMAGE],
                ["webp", CONTENT_TYPE_IMAGE],

                ["mp3", CONTENT_TYPE_AUDIO],
                ["wav", CONTENT_TYPE_AUDIO],
                ["ogg", CONTENT_TYPE_AUDIO],

                ["mp4", CONTENT_TYPE_VIDEO],
                ["ogv", CONTENT_TYPE_VIDEO],
                ["mp4", CONTENT_TYPE_VIDEO],
                ["webm", CONTENT_TYPE_VIDEO],

                ["js", CONTENT_TYPE_SCRIPT],
                ["html", CONTENT_TYPE_SCRIPT],
                ["css", CONTENT_TYPE_SCRIPT],
                ["ts", CONTENT_TYPE_SCRIPT],
                ["json", CONTENT_TYPE_SCRIPT],

                ["txt", CONTENT_TYPE_TEXT],
                ["md", CONTENT_TYPE_TEXT],
            ]);

        }
    }
}