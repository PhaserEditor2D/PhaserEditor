namespace colibri.ui.controls {

    export class Menu {

        private _actions: Action[];
        private _element: HTMLUListElement;
        private _bgElement: HTMLDivElement;
        private _menuCloseCallback: () => void;

        constructor() {
            this._actions = [];
        }

        setMenuClosedCallback(callback: () => void) {
            this._menuCloseCallback = callback;
        }

        add(action: Action) {
            this._actions.push(action);
        }

        isEmpty() {
            return this._actions.length === 0;
        }

        getElement() {
            return this._element;
        }

        create() {

            this._element = document.createElement("ul");
            this._element.classList.add("Menu");

            for (const action of this._actions) {

                const item = document.createElement("li");

                item.classList.add("MenuItem");

                item.innerText = action.getText();

                if (action.isEnabled()) {

                    item.addEventListener("click", e => {
                        this.close();
                        action.run();
                    });

                } else {

                    item.classList.add("MenuItemDisabled");
                }

                this._element.appendChild(item);
            }

            this._bgElement = document.createElement("div");

            this._bgElement.classList.add("MenuContainer");

            this._bgElement.addEventListener("mousedown", e => {
                
                e.preventDefault();
                e.stopImmediatePropagation();

                this.close();
            });

            document.body.appendChild(this._bgElement);

            document.body.appendChild(this._element);
        }

        close() {

            this._bgElement.remove();
            this._element.remove();

            if (this._menuCloseCallback) {
                this._menuCloseCallback();
            }
        }
    }
}