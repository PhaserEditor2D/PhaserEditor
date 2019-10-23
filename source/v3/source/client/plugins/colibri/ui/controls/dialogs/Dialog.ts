namespace colibri.ui.controls.dialogs {

    export class Dialog extends Control {

        private _containerElement: HTMLElement;
        private _width: number;
        private _height: number;
        private static _dialogs: Dialog[] = [];
        private static _firstTime = true;

        constructor(...classList : string[]) {
            super("div", "Dialog", ...classList);

            if (Dialog._firstTime) {
                Dialog._firstTime = false;
                window.addEventListener("keydown", e => {
                    if (e.code === "Escape") {
                        const list = Dialog._dialogs;
                        if (list.length > 0) {
                            const dlg = list.pop();
                            dlg.close();
                        }
                    }
                })
            }

            Dialog._dialogs.push(this);
        }

        create() {
            this._containerElement = document.createElement("div");
            this._containerElement.classList.add("DialogContainer")

            document.body.appendChild(this._containerElement);
            document.body.appendChild(this.getElement());

            this._width = 400;
            this._height = 300;

            window.addEventListener("resize", () => this.resize());

            this.createDialogArea();

            this.resize();
        }

        protected createDialogArea() {

        }

        protected resize() {

            this.setBounds({
                x: window.innerWidth / 2 - this._width / 2,
                y: window.innerHeight * 0.2,
                width: this._width,
                height: this._height
            });
        }

        close() {

            this._containerElement.remove();
            this.getElement().remove();
        }

    }

}