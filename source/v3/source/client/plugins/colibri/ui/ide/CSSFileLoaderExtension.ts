namespace colibri.ui.ide {

    export class CSSFileLoaderExtension extends core.extensions.Extension {

        static POINT_ID = "colibri.ui.ide.CSSFileLoaderExtension";

        private _cssUrls: string[];

        constructor(id, cssUrls: string[], priority: number = 10) {
            super(id, priority);

            this._cssUrls = cssUrls;
        }

        getCSSUrls() {
            return this._cssUrls;
        }

    }

}