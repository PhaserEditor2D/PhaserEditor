namespace phasereditor2d.ui.controls {

    export const EVENT_TAB_CLOSED = "tabClosed";
    export const EVENT_TAB_SELECTED = "tabSelected";

    export class TabPane extends Control {
        private _selectionHistoryLabelElement: HTMLElement[];
        private _titleBarElement: HTMLElement;
        private _contentAreaElement: HTMLElement;

        constructor(...classList: string[]) {
            super("div", "TabPane", ...classList);


            this._selectionHistoryLabelElement = [];

            this._titleBarElement = document.createElement("div");
            this._titleBarElement.classList.add("TabPaneTitleBar");
            this.getElement().appendChild(this._titleBarElement);

            this._contentAreaElement = document.createElement("div");
            this._contentAreaElement.classList.add("TabPaneContentArea");
            this.getElement().appendChild(this._contentAreaElement);
        }

        public addTab(label: string, content: Control, closeable = false): void {
            const labelElement = this.makeLabel(label, closeable);
            this._titleBarElement.appendChild(labelElement);
            labelElement.addEventListener("click", e => this.selectTab(labelElement));

            const contentArea = new Control("div", "ContentArea");
            contentArea.add(content);
            this._contentAreaElement.appendChild(contentArea.getElement());

            labelElement["__contentArea"] = contentArea.getElement();

            if (this._titleBarElement.childElementCount === 1) {
                this.selectTab(labelElement);
            }
        }

        private makeLabel(label: string, closeable: boolean): HTMLElement {
            const labelElement = document.createElement("div");
            labelElement.classList.add("TabPaneLabel");

            const tabIconElement = document.createElement("canvas");
            tabIconElement.width = 16;
            tabIconElement.height = 16;
            tabIconElement.style.width = tabIconElement.width + "px";
            tabIconElement.style.height = tabIconElement.height + "px";

            labelElement.appendChild(tabIconElement);

            const textElement = document.createElement("span");
            textElement.innerHTML = label;
            labelElement.appendChild(textElement);

            if (closeable) {
                const closeIconElement = Controls.createIconElement("close");
                closeIconElement.classList.add("closeIcon");
                closeIconElement.addEventListener("click", e => {
                    e.stopImmediatePropagation();
                    this.closeTab(labelElement);
                });

                labelElement.appendChild(closeIconElement);
                labelElement.classList.add("closeable");
            }

            return labelElement;
        }

        private closeTab(labelElement: HTMLElement): void {
            this._titleBarElement.removeChild(labelElement);
            const contentArea = <HTMLElement>labelElement["__contentArea"];
            this._contentAreaElement.removeChild(contentArea);

            let toSelectLabel: HTMLElement = null;

            const selectedLabel = this.getSelectedLabelElement();
            if (selectedLabel === labelElement) {
                this._selectionHistoryLabelElement.pop();
                const nextInHistory = this._selectionHistoryLabelElement.pop();;
                if (nextInHistory) {
                    toSelectLabel = nextInHistory;
                } else {
                    if (this._titleBarElement.childElementCount > 0) {
                        toSelectLabel = <HTMLElement>this._titleBarElement.firstChild;
                    }
                }
            }

            this.dispatchEvent(new CustomEvent(EVENT_TAB_CLOSED, {
                detail: Control.getControlOf(<HTMLElement>contentArea.firstChild)
            }));

            if (toSelectLabel) {
                this.selectTab(toSelectLabel);
            }
        }

        public setTabTitle(content: Control, title: string, icon? : IIcon) {
            for (let i = 0; i < this._titleBarElement.childElementCount; i++) {
                const label = <HTMLElement>this._titleBarElement.children.item(i);
                const content2 = this.getContentFromLabel(label);
                if (content2 === content) {
                    const iconElement : HTMLCanvasElement = <HTMLCanvasElement> label.firstChild;
                    const textElement = <HTMLElement> iconElement.nextSibling;
                    if (icon) {
                        icon.paint(iconElement.getContext("2d"), 0, 0);
                    }
                    textElement.innerHTML = title;
                }
            }
        }

        private getLabelFromContent(content : Control) {
            for (let i = 0; i < this._titleBarElement.childElementCount; i++) {
                const label = <HTMLElement>this._titleBarElement.children.item(i);
                const content2 = this.getContentFromLabel(label);
                if (content2 === content) {
                    return label;
                }
            }
            return null;
        }

        private getContentAreaFromLabel(labelElement: HTMLElement): HTMLElement {
            return labelElement["__contentArea"];
        }

        private getContentFromLabel(labelElement: HTMLElement) {
            return Control.getControlOf(<HTMLElement>this.getContentAreaFromLabel(labelElement).firstChild);
        }

        public selectTabWithContent(content : Control) {
            const label = this.getLabelFromContent(content);
            if (label) {
                this.selectTab(label);
            }
        }

        private selectTab(toSelectLabel: HTMLElement): void {
            const selectedLabel = this._selectionHistoryLabelElement.pop();

            if (selectedLabel) {
                if (selectedLabel === toSelectLabel) {
                    this._selectionHistoryLabelElement.push(selectedLabel);
                    return;
                }
                selectedLabel.classList.remove("selected");
                const selectedContentArea = this.getContentAreaFromLabel(selectedLabel);
                selectedContentArea.classList.remove("selected");
            }

            toSelectLabel.classList.add("selected");
            const toSelectContentArea = this.getContentAreaFromLabel(toSelectLabel);
            toSelectContentArea.classList.add("selected");
            this._selectionHistoryLabelElement.push(toSelectLabel);

            this.dispatchEvent(new CustomEvent(EVENT_TAB_SELECTED, {
                detail: this.getContentFromLabel(toSelectLabel)
            }));

            this.dispatchLayoutEvent();
        }

        public getSelectedTabContent(): Control {
            const label = this.getSelectedLabelElement();
            if (label) {
                const area = this.getContentAreaFromLabel(label);
                return Control.getControlOf(<HTMLElement>area.firstChild);
            }
            return null;
        }

        public getContentList(): controls.Control[] {
            const list: controls.Control[] = [];

            for (let i = 0; i < this._titleBarElement.children.length; i++) {
                const label = <HTMLElement>this._titleBarElement.children.item(i);
                const content = this.getContentFromLabel(label);
                list.push(content);
            }

            return list;
        }


        private getSelectedLabelElement(): HTMLElement {
            return this._selectionHistoryLabelElement.length > 0 ?
                this._selectionHistoryLabelElement[this._selectionHistoryLabelElement.length - 1]
                : null;
        }
    }

}