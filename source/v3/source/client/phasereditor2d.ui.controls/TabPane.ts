namespace phasereditor2d.ui.controls {

    export declare type GetContent = () => Control;

    export class TabPane extends Control {

        private _tabLabelList: HTMLElement[];
        private _tabContentList: Control[];
        private _tabGetContentList: GetContent[];
        private _selectedIndex: number;
        private _titleBarElement: HTMLElement;
        private _contentAreaElement: HTMLElement;

        constructor(...classList: string[]) {
            super("div", "TabPane", ...classList);

            this._tabLabelList = [];
            this._tabContentList = [];
            this._tabGetContentList = [];
            this._selectedIndex = -1;

            this._titleBarElement = document.createElement("div");
            this._titleBarElement.classList.add("TabPaneTitleBar");
            this.getElement().appendChild(this._titleBarElement);

            this._contentAreaElement = document.createElement("div");
            this._contentAreaElement.classList.add("TabPaneContentArea");
            this.getElement().appendChild(this._contentAreaElement);
        }

        addTab(label: string, getContent: GetContent, closeable = false) {
            {
                const elem = this.makeLabel(label, closeable);
                this._tabLabelList.push(elem);
                this._titleBarElement.appendChild(elem);
                const index = this._tabLabelList.length;
                elem.addEventListener("click", e => this.selectTab(index - 1));
            }

            this._tabContentList.push(null);
            this._tabGetContentList.push(getContent);

            if (this.getCountTabs() === 1) {
                this.selectTab(0);
            }
        }
        
        private makeLabel(label: string, closeable : boolean) {
            const labelElement = document.createElement("div");
            labelElement.classList.add("TabPaneLabel");

            const textElement = document.createElement("span"); 
            textElement.innerHTML = label;
            labelElement.appendChild(textElement);
            
            if (closeable) {
                const iconElement = Controls.createIconElement("close");
                iconElement.classList.add("closeIcon");
                labelElement.appendChild(iconElement);
                labelElement.classList.add("closeable");
            } 

            console.log(labelElement.innerHTML);

            return labelElement;
        }

        getCountTabs() {
            return this._tabContentList.length;
        }

        selectTab(index: number): void {
            if (this._selectedIndex >= 0) {
                this._tabLabelList[this._selectedIndex].classList.remove("selected");
                this._tabContentList[this._selectedIndex].removeClass("selected");
            }

            if (!this._tabContentList[index]) {
                const content = this.createTabContent(index);
                this._tabContentList[index] = content;
                this._contentAreaElement.appendChild(content.getElement());
            }

            this._tabLabelList[index].classList.add("selected");
            this._tabContentList[index].addClass("selected");
            this._selectedIndex = index;
        }

        getSelectedTabContent() {
            if (this._selectedIndex >= 0) {
                return this._tabContentList[this._selectedIndex];
            }
            return null;
        }

        private createTabContent(index: number) {
            const contentArea = new Control("div", "ContentArea");
            contentArea.add(this._tabGetContentList[index]());
            return contentArea;
        }

    }

}