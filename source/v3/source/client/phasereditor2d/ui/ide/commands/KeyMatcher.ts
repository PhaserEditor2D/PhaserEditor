namespace phasereditor2d.ui.ide.commands {

    export class KeyMatcher {

        private _control: boolean;
        private _shift: boolean;
        private _alt: boolean;
        private _meta: boolean;
        private _key: string;

        constructor(config: {
            control?: boolean,
            shift?: boolean,
            alt?: boolean,
            meta?: boolean,
            key?: string
        }) {

            this._control = config.control === undefined ? false : config.control;
            this._shift = config.shift === undefined ? false : config.shift;
            this._alt = config.alt === undefined ? false : config.alt;
            this._meta = config.meta === undefined ? false : config.meta;
            this._key = config.key === undefined ? "" : config.key;

        }

        matches(event: KeyboardEvent) {
            return event.ctrlKey === this._control
                && event.shiftKey === this._shift
                && event.altKey === this._alt
                && event.metaKey === this._meta
                && event.key.toLowerCase() === this._key.toLowerCase();
        }
    }

}