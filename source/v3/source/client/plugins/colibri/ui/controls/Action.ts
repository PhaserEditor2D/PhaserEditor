namespace colibri.ui.controls {

    export const EVENT_ACTION_CHANGED = "actionChanged";

    export class Action extends EventTarget {

        private _text: string;
        private _icon: IImage;
        private _callback: () => void;

        constructor(config: {
            text?: string,
            icon?: IImage,
            callback?: () => void
        }) {
            super();

            this._text = config.text || "";
            this._icon = config.icon || null;
            this._callback = config.callback || null;
        }

        getText() {
            return this._text;
        }

        getIcon() {
            return this._icon;
        }

        run() {

            if (this._callback) {
                this._callback();
            }
        }

    }
}