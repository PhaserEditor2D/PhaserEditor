namespace phasereditor2d.ui.controls {
    export class PaddingPane extends Control {
        private _padding: number;
        private _control: Control;

        constructor(control?: Control, padding: number = 5) {
            super();
            this._padding = padding;
            this.getElement().classList.add("paddingPane");
            this.setControl(control);
        }

        setControl(control: Control) {
            this._control = control;
            if (this._control) {
                this.add(control);
            }
        }

        getControl() {
            return this._control;
        }

        setPadding(padding: number) {
            this._padding = padding;
        }

        getPadding() {
            return this._padding;
        }

        layout() {
            const b = this.getBounds();

            setElementBounds(this.getElement(), b);

            if (this._control) {
                this._control.setBoundsValues(this._padding, this._padding, b.width - this._padding * 2, b.height - this._padding * 2);
            }
        }
    }
}