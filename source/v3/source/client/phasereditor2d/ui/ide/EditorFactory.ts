namespace phasereditor2d.ui.ide {

    export abstract class EditorFactory {

        private _id: string;

        public constructor(id: string) {
            this._id = id;
        }

        public getId() {
            return this._id;
        }

        public abstract acceptInput(input : any) : boolean;

        public abstract createEditor(): EditorPart;

    }
}