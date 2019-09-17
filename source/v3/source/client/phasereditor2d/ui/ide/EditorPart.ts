namespace phasereditor2d.ui.ide {

    export class EditorPart extends Part {

        private _input : any;

        public constructor(id: string) {
            super(id);
            this.addClass("EditorPart");
        }

        public getInput() {
            return this._input;
        }

        public setInput(input : any) : void {
            this._input = input;
        }

    }
}