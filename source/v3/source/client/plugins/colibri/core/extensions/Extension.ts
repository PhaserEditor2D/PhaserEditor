namespace colibri.core.extensions {

    export class Extension {

        private _id: string;
        private _priority: number;

        constructor(id: string, priority: number = 10) {
            this._id = id;
            this._priority = priority;
        }

        getId() {
            return this._id;
        }

        getPriority() {
            return this._priority;
        }

    }

}