namespace phasereditor2d.ui.controls {

    class PanelTitle extends Control {
        private _textControl: Control;
        private _toolbar: PanelToolbar;


        constructor() {
            super("div", "panelTitle");

            this._textControl = new Control();
            this.add(this._textControl);

            this._toolbar = new PanelToolbar();
            this.add(this._toolbar);
        }

        setText(text: string) {
            this._textControl.getElement().innerHTML = text;
        }

        getToolbar(): IToolbar {
            return this._toolbar;
        }

        layout() {
            super.layout();

            const b = this.getBounds();

            const elem = this._textControl.getElement();
            elem.style.top = FONT_OFFSET + "px";
            elem.style.left = FONT_OFFSET * 2 + "px";

            const toolbarWidth = this._toolbar.getActions().length * ACTION_WIDTH;
            this._toolbar.setBoundsValues(b.width - toolbarWidth, 0, toolbarWidth, ROW_HEIGHT);

        }
    }

    class PanelToolbar extends Control implements IToolbar {
        private _actions: Action[];
        private _buttons: ActionButton[];

        constructor() {
            super("div", "panelToolbar");
            this._actions = [];
            this._buttons = [];
        }

        addAction(action: Action) {
            this._actions.push(action);
            const b = new ActionButton(action);
            this._buttons.push(b);
            this.add(b);
        }

        getActions() {
            return this._actions;
        }

        layout() {
            super.layout();

            const b = this.getBounds();

            for (let i = 0; i < this._buttons.length; i++) {
                const btn = this._buttons[i];
                btn.setBoundsValues(i * ACTION_WIDTH, 0, ACTION_WIDTH, b.height);
            }
        }
    }

    export class Panel extends Control {
        private _clientArea: Control;
        private _panelTitle: PanelTitle;
        private _title: string;

        constructor(hasTitle: boolean = true) {
            super();

            this.getElement().classList.add("panel");

            if (hasTitle) {
                this._panelTitle = new PanelTitle();
                this.add(this._panelTitle);
            }

            this._clientArea = new Control("div");
            this._clientArea.addClass("panelClientArea");

            this.add(this._clientArea);
        }

        setTitle(title: string) {
            this._title = title;
            this._panelTitle.setText(title);
        }

        getTitle() {
            return this._title;
        }

        getToolbar(): IToolbar {
            return this._panelTitle.getToolbar();
        }

        getClientArea() {
            return this._clientArea;
        }

        layout() {
            //super.layout();
            setElementBounds(this.getElement(), this.getBounds());

            const b = this.getBounds();

            if (this._panelTitle) {
                this._panelTitle.setBoundsValues(PANEL_BORDER_SIZE, PANEL_BORDER_SIZE, b.width - PANEL_BORDER_SIZE * 2, ROW_HEIGHT);

                this._clientArea.setBounds({
                    x: PANEL_BORDER_SIZE,
                    y: PANEL_BORDER_SIZE + ROW_HEIGHT,
                    width: b.width - PANEL_BORDER_SIZE * 2,
                    height: b.height - PANEL_BORDER_SIZE * 2 - ROW_HEIGHT
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