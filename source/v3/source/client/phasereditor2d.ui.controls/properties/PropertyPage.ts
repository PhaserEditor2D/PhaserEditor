namespace phasereditor2d.ui.controls.properties {

    class PropertySectionPane extends Control {

        private _section: PropertySection<any>;
        private _titleArea: HTMLDivElement;
        private _expandBtn: HTMLElement;
        private _formArea: HTMLDivElement;
        private _page: PropertyPage;

        constructor(page: PropertyPage, section: PropertySection<any>) {
            super();
            this._page = page;
            this._section = section;

            this.addClass("PropertySectionPane")

        }

        createOrUpdateWithSelection() {
            if (!this._formArea) {

                this._titleArea = document.createElement("div");
                this._titleArea.classList.add("PropertyTitleArea");

                this._expandBtn = document.createElement("div");
                this._expandBtn.classList.add("expandBtn", "expanded");
                this._expandBtn.addEventListener("mouseup", () => this.toggleSection());
                this._titleArea.appendChild(this._expandBtn);

                const label = document.createElement("label");
                label.innerText = this._section.getTitle();
                label.addEventListener("mouseup", () => this.toggleSection());
                this._titleArea.appendChild(label);


                this._formArea = document.createElement("div");
                this._formArea.classList.add("PropertyFormArea");
                this._section.create(this._formArea);

                this.getElement().appendChild(this._titleArea);
                this.getElement().appendChild(this._formArea);
            }

            this._section.updateWithSelection();
        }

        isExpanded() {
            return this._expandBtn.classList.contains("expanded");
        }

        private toggleSection(): void {
            if (this.isExpanded()) {
                this._expandBtn.classList.remove("expanded");
                this._expandBtn.classList.add("collapsed");
                this._formArea.style.display = "none";
            } else {
                this._expandBtn.classList.add("expanded");
                this._expandBtn.classList.remove("collapsed");
                this._formArea.style.display = "initial";
            }
            this._page.updateExpandStatus();
            this.getContainer().dispatchLayoutEvent();
        }

        getSection() {
            return this._section;
        }

        getFormArea() {
            return this._formArea;
        }
    }

    export class PropertyPage extends Control {
        private _sectionProvider: PropertySectionProvider;
        private _sectionPanes: PropertySectionPane[];
        private _sectionPaneMap: Map<String, PropertySectionPane>;
        private _selection: any[];

        constructor() {
            super("div");

            this.addClass("PropertyPage");

            this._sectionPanes = [];
            this._sectionPaneMap = new Map();
            this._selection = [];
        }

        private build() {

            if (this._sectionProvider) {

                const list: PropertySection<any>[] = [];

                this._sectionProvider.addSections(this, list);

                for (const section of list) {
                    if (!this._sectionPaneMap.has(section.getId())) {
                        const pane = new PropertySectionPane(this, section);
                        this.add(pane);
                        this._sectionPaneMap.set(section.getId(), pane);
                        this._sectionPanes.push(pane);
                    }
                }
            }

            this.updateWithSelection();
        }

        private updateWithSelection(): void {

            const n = this._selection.length;

            for (const pane of this._sectionPanes) {

                const section = pane.getSection();
                let show = false;
                if (section.canEditNumber(n)) {
                    show = true;
                    for (const obj of this._selection) {
                        if (!section.canEdit(obj, n)) {
                            show = false;
                            break;
                        }
                    }
                }

                if (show) {
                    pane.getElement().style.display = "grid";
                    pane.createOrUpdateWithSelection();
                } else {
                    pane.getElement().style.display = "none";
                }
            }

            this.updateExpandStatus();

        }


        updateExpandStatus() {
            let templateRows = "";

            for (const pane of this._sectionPanes) {
                if (pane.style.display !== "none") {
                    pane.createOrUpdateWithSelection();
                    if (pane.isExpanded()) {
                        templateRows += " " + (pane.getSection().isFillSpace() ? "1fr" : "min-content");
                    } else {
                        templateRows += " min-content";
                    }
                }
            }

            this.getElement().style.gridTemplateRows = templateRows + " ";
        }

        getSelection() {
            return this._selection;
        }

        setSelection(sel: any[]): any {
            this._selection = sel;

            this.updateWithSelection();
        }

        setSectionProvider(provider: PropertySectionProvider): void {
            this._sectionProvider = provider;

            this.build();
        }

        getSectionProvider() {
            return this._sectionProvider;
        }
    }

}