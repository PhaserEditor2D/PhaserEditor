namespace colibri.ui.controls {

    export const EVENT_TAB_CLOSED = "tabClosed";
    export const EVENT_TAB_SELECTED = "tabSelected";

    class CloseIconManager {

        private _element: HTMLCanvasElement;
        private _context: CanvasRenderingContext2D;
        private _icon: IImage;
        private _overIcon: IImage;

        constructor() {

            this._element = document.createElement("canvas");
            this._element.classList.add("closeIcon");
            this._element.width = ICON_SIZE;
            this._element.height = ICON_SIZE;
            this._element.style.width = ICON_SIZE + "px";
            this._element.style.height = ICON_SIZE + "px";

            this._context = this._element.getContext("2d");

            this._element.addEventListener("mouseenter", e => {
                this.paint(this._overIcon);
            });

            this._element.addEventListener("mouseleave", e => {
                this.paint(this._icon);
            });
        }

        setIcon(icon: IImage) {
            this._icon = icon;
        }

        setOverIcon(icon: IImage) {
            this._overIcon = icon;
        }

        getElement() {
            return this._element;
        }

        repaint() {
            this.paint(this._icon);
        }

        paint(icon: IImage) {

            if (icon) {

                this._context.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
                icon.paint(this._context, 0, 0, ICON_SIZE, ICON_SIZE, true);

            }
        }
    }

    export class TabPane extends Control {

        private _selectionHistoryLabelElement: HTMLElement[];
        private _titleBarElement: HTMLElement;
        private _contentAreaElement: HTMLElement;
        private _iconSize: number;

        constructor(...classList: string[]) {
            super("div", "TabPane", ...classList);

            this._selectionHistoryLabelElement = [];

            this._titleBarElement = document.createElement("div");
            this._titleBarElement.classList.add("TabPaneTitleBar");
            this.getElement().appendChild(this._titleBarElement);

            this._contentAreaElement = document.createElement("div");
            this._contentAreaElement.classList.add("TabPaneContentArea");
            this.getElement().appendChild(this._contentAreaElement);

            this._iconSize = ICON_SIZE;
        }

        addTab(label: string, icon: IImage, content: Control, closeable = false): void {

            const labelElement = this.makeLabel(label, icon, closeable);
            this._titleBarElement.appendChild(labelElement);
            labelElement.addEventListener("mousedown", e => this.selectTab(labelElement));

            const contentArea = new Control("div", "ContentArea");
            contentArea.add(content);
            this._contentAreaElement.appendChild(contentArea.getElement());

            labelElement["__contentArea"] = contentArea.getElement();

            if (this._titleBarElement.childElementCount === 1) {
                this.selectTab(labelElement);
            }
        }

        incrementTabSize(amount: number) {

            this._iconSize = Math.max(ICON_SIZE, this._iconSize + amount);

            for (let i = 0; i < this._titleBarElement.children.length; i++) {

                const label = this._titleBarElement.children.item(i);

                const iconCanvas = label.firstChild as HTMLCanvasElement;

                iconCanvas.width = this._iconSize;
                iconCanvas.height = this._iconSize;

                iconCanvas.style.width = this._iconSize + "px";
                iconCanvas.style.height = this._iconSize + "px";

                iconCanvas.dispatchEvent(new CustomEvent("repaint", {}));

                this.layout();
            }
        }

        private makeLabel(label: string, icon: IImage, closeable: boolean): HTMLElement {

            const labelElement = document.createElement("div");
            labelElement.classList.add("TabPaneLabel");

            const tabIconElement = Controls.createIconElement(icon, null, this._iconSize);

            labelElement.appendChild(tabIconElement);

            const textElement = document.createElement("span");
            textElement.innerHTML = label;
            labelElement.appendChild(textElement);

            if (closeable) {

                const manager = new CloseIconManager();
                manager.setIcon(Controls.getIcon(ICON_CONTROL_CLOSE));
                manager.repaint();
                labelElement.appendChild(manager.getElement());
                labelElement.classList.add("closeable");
                labelElement["__CloseIconManager"] = manager;
                manager.getElement().addEventListener("click", e => {
                    e.stopImmediatePropagation();
                    this.closeTabLabel(labelElement);
                });
            }

            return labelElement;
        }

        setTabCloseIcons(labelElement: HTMLElement, icon: IImage, overIcon: IImage) {

            const manager = <CloseIconManager>labelElement["__CloseIconManager"];

            if (manager) {

                manager.setIcon(icon);
                manager.setOverIcon(overIcon);
                manager.repaint();
            }
        }


        closeTab(content: controls.Control) {

            const label = this.getLabelFromContent(content);

            if (label) {
                this.closeTabLabel(label);
            }
        }

        private closeTabLabel(labelElement: HTMLElement): void {
            {
                const content = TabPane.getContentFromLabel(labelElement);
                const event = new CustomEvent(EVENT_TAB_CLOSED, {
                    detail: content,
                    cancelable: true
                });

                if (!this.dispatchEvent(event)) {
                    return;
                }
            }


            this._titleBarElement.removeChild(labelElement);
            const contentArea = <HTMLElement>labelElement["__contentArea"];
            this._contentAreaElement.removeChild(contentArea);

            let toSelectLabel: HTMLElement = null;

            const selectedLabel = this.getSelectedLabelElement();

            if (selectedLabel === labelElement) {

                this._selectionHistoryLabelElement.pop();
                const nextInHistory = this._selectionHistoryLabelElement.pop();

                if (nextInHistory) {

                    toSelectLabel = nextInHistory;

                } else {

                    if (this._titleBarElement.childElementCount > 0) {

                        toSelectLabel = <HTMLElement>this._titleBarElement.firstChild;
                    }
                }
            }

            if (toSelectLabel) {
                this.selectTab(toSelectLabel);
            }
        }

        setTabTitle(content: Control, title: string, icon?: IImage) {

            for (let i = 0; i < this._titleBarElement.childElementCount; i++) {

                const label = <HTMLElement>this._titleBarElement.children.item(i);

                const content2 = TabPane.getContentFromLabel(label);

                if (content2 === content) {

                    const iconElement: HTMLCanvasElement = <HTMLCanvasElement>label.firstChild;
                    const textElement = <HTMLElement>iconElement.nextSibling;

                    if (icon) {

                        const context = iconElement.getContext("2d");
                        context.clearRect(0, 0, iconElement.width, iconElement.height);
                        icon.paint(context, 0, 0, iconElement.width, iconElement.height, false);
                    }

                    textElement.innerHTML = title;
                }
            }
        }

        static isTabLabel(element: HTMLElement) {
            return element.classList.contains("TabPaneLabel");
        }

        getLabelFromContent(content: Control) {

            for (let i = 0; i < this._titleBarElement.childElementCount; i++) {

                const label = <HTMLElement>this._titleBarElement.children.item(i);

                const content2 = TabPane.getContentFromLabel(label);

                if (content2 === content) {
                    return label;
                }
            }

            return null;
        }

        private static getContentAreaFromLabel(labelElement: HTMLElement): HTMLElement {
            return labelElement["__contentArea"];
        }

        static getContentFromLabel(labelElement: HTMLElement) {
            return Control.getControlOf(<HTMLElement>this.getContentAreaFromLabel(labelElement).firstChild);
        }

        selectTabWithContent(content: Control) {

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
                const selectedContentArea = TabPane.getContentAreaFromLabel(selectedLabel);
                selectedContentArea.classList.remove("selected");
            }

            toSelectLabel.classList.add("selected");
            const toSelectContentArea = TabPane.getContentAreaFromLabel(toSelectLabel);
            toSelectContentArea.classList.add("selected");
            this._selectionHistoryLabelElement.push(toSelectLabel);

            this.dispatchEvent(new CustomEvent(EVENT_TAB_SELECTED, {
                detail: TabPane.getContentFromLabel(toSelectLabel)
            }));

            this.dispatchLayoutEvent();
        }

        getSelectedTabContent(): Control {

            const label = this.getSelectedLabelElement();

            if (label) {

                const area = TabPane.getContentAreaFromLabel(label);

                return Control.getControlOf(<HTMLElement>area.firstChild);
            }

            return null;
        }

        getContentList(): controls.Control[] {

            const list: controls.Control[] = [];

            for (let i = 0; i < this._titleBarElement.children.length; i++) {

                const label = <HTMLElement>this._titleBarElement.children.item(i);

                const content = TabPane.getContentFromLabel(label);

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