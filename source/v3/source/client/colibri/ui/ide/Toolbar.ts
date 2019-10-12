namespace colibri.ui.toolbar {
    export class Toolbar {
        
        private _toolbarElement : HTMLDivElement;

        constructor() {
            this._toolbarElement = document.createElement("div");
            this._toolbarElement.innerHTML = `

            <button>Load</button>
            <button>Play</button>

            `;
            
            this._toolbarElement.classList.add("toolbar");

            document.getElementsByTagName("body")[0].appendChild(this._toolbarElement);
        }

        getElement() {
            return this._toolbarElement;
        }
    }
}