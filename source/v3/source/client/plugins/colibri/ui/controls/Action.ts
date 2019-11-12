namespace colibri.ui.controls {

    export const EVENT_ACTION_CHANGED = "actionChanged";

    export class Action extends EventTarget {

        private _text: string;
        private _icon: IImage;
        private _enabled: boolean;
        private _callback: () => void;

        constructor(config: {
            text?: string,
            icon?: IImage,
            enabled?: boolean
            callback?: () => void
        }) {
            super();

            this._text = config.text ?? "";
            this._icon = config.icon ?? null;
            this._enabled = config.enabled === undefined || config.enabled;
            this._callback = config.callback ?? null;
        }

        isEnabled() {
            return this._enabled;
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