namespace colibri.ui.ide {

    export class EditorExtension extends core.extensions.Extension {

        static POINT_ID = "colibri.ui.ide.EditorExtension";

        private _factories: EditorFactory[];

        constructor(id: string, factories: EditorFactory[]) {
            super(id);

            this._factories = factories;
        }

        getFactories() {
            return this._factories;
        }

    }

}