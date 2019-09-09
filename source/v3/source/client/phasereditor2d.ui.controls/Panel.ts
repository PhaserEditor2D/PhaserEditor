namespace phasereditor2d.ui.controls {

    class PanelTitle extends Control {
        private _textControl: Control;


        constructor() {
            super("div", "PanelTitle");
            this._textControl = new Control("label", "PanelTitleText");
            this.setLayoutChildren(false);
            this.add(this._textControl);
        }

        setText(text: string) {
            this._textControl.getElement().innerHTML = text;
        }
    }

    export class Panel extends Control {
        private _clientArea: Control;
        private _panelTitle: PanelTitle;
        private _title: string;

        constructor(hasTitle: boolean = true) {
            super("div", "Panel");

            if (hasTitle) {
                this._panelTitle = new PanelTitle();
                this.add(this._panelTitle);
            }

            this._clientArea = new Control("div");
            this._clientArea.addClass("PanelClientArea");

            this.add(this._clientArea);
        }

        setTitle(title: string) {
            this._title = title;
            this._panelTitle.setText(title);
        }

        getTitle() {
            return this._title;
        }

        getClientArea() {
            return this._clientArea;
        }

        layout() {
            //super.layout();
            setElementBounds(this.getElement(), this.getBounds());

            const b = this.getBounds();

            if (this._panelTitle) {
                this._panelTitle.setBoundsValues(PANEL_BORDER_SIZE, PANEL_BORDER_SIZE, b.width - PANEL_BORDER_SIZE * 2, PANEL_TITLE_HEIGHT);

                this._clientArea.setBounds({
                    x: PANEL_BORDER_SIZE,
                    y: PANEL_BORDER_SIZE + PANEL_TITLE_HEIGHT,
                    width: b.width - PANEL_BORDER_SIZE * 2,
                    height: b.height - PANEL_BORDER_SIZE * 2 - PANEL_TITLE_HEIGHT
                });
            } else {
                this._clientArea.setBounds({
                    x: PANEL_BORDER_SIZE,
                    y: PANEL_BORDER_SIZE,
                    width: b.width - PANEL_BORDER_SIZE * 2,
                    height: b.height - PANEL_BORDER_SIZE * 2
                });
            }
        }
    }
}