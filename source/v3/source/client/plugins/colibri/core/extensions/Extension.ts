namespace colibri.core.extensions {

    export class Extension {

        private _extensionPoint;
        private _priority: number;

        constructor(priority: number = 10) {
            this._priority = priority;
        }

        getPriority() {
            return this._priority;
        }
    }
}