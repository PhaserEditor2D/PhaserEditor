declare namespace Phaser.Types.Tweens {

    class StaggerBuilderConfig { }

}

declare class ActiveXObject { }

declare namespace Phaser.Loader.FileTypes {

    type MultiAtlasFileConfig = {
        /**
         * The key of the file. Must be unique within both the Loader and the Texture Manager.
         */
        key: string;
        /**
         * The absolute or relative URL to load the multi atlas json file from. Or, a well formed JSON object.
         */
        url?: string;
        /**
         * The default file extension to use for the atlas json if no url is provided.
         */
        atlasExtension?: string;
        /**
         * Extra XHR Settings specifically for the atlas json file.
         */
        atlasXhrSettings?: Phaser.Types.Loader.XHRSettingsObject;
        /**
         * Optional path to use when loading the textures defined in the atlas data.
         */
        path?: string;
        /**
         * Optional Base URL to use when loading the textures defined in the atlas data.
         */
        baseURL?: string;
        /**
         * Extra XHR Settings specifically for the texture files.
         */
        textureXhrSettings?: Phaser.Types.Loader.XHRSettingsObject;
    };

    
    type ImageFileConfig = {
        /**
         * The key of the file. Must be unique within both the Loader and the Texture Manager.
         */
        key: string;
        /**
         * The absolute or relative URL to load the file from.
         */
        url?: string;
        /**
         * The default file extension to use if no url is provided.
         */
        extension?: string;
        /**
         * The filename of an associated normal map. It uses the same path and url to load as the image.
         */
        normalMap?: string;
        /**
         * The frame configuration object. Only provided for, and used by, Sprite Sheets.
         */
        frameConfig?: Phaser.Types.Loader.FileTypes.ImageFrameConfig;
        /**
         * Extra XHR Settings specifically for this file.
         */
        xhrSettings?: Phaser.Types.Loader.XHRSettingsObject;
    };
}

