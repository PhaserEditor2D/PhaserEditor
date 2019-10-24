/// <reference path="./ExtensionContentTypeResolver.ts" />

namespace phasereditor2d.files.core {

    export const CONTENT_TYPE_IMAGE = "image";
    export const CONTENT_TYPE_SVG = "svg";
    export const CONTENT_TYPE_AUDIO = "audio";
    export const CONTENT_TYPE_VIDEO = "video";
    export const CONTENT_TYPE_SCRIPT = "script";
    export const CONTENT_TYPE_TEXT = "text";
    export const CONTENT_TYPE_CSV = "csv";

    export class DefaultExtensionTypeResolver extends ExtensionContentTypeResolver {
        constructor() {
            super("phasereditor2d.files.core.DefaultExtensionTypeResolver", [
                ["png", CONTENT_TYPE_IMAGE],
                ["jpg", CONTENT_TYPE_IMAGE],
                ["bmp", CONTENT_TYPE_IMAGE],
                ["gif", CONTENT_TYPE_IMAGE],
                ["webp", CONTENT_TYPE_IMAGE],

                ["svg", CONTENT_TYPE_SVG],

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

                ["csv", CONTENT_TYPE_CSV]
            ]);

        }
    }

}