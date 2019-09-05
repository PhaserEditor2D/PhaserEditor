namespace phasereditor2d.ui.controls.properties {

    class PropertySectionPane extends Control {

        private _section: PropertySection<any>;
        private _titleArea: HTMLDivElement;
        private _expandBtn: HTMLElement;
        private _formArea: HTMLDivElement;

        constructor(section: PropertySection<any>) {
            super();

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

        private toggleSection() : void {
            if (this._expandBtn.classList.contains("expanded")) {
                this._expandBtn.classList.remove("expanded");
                this._expandBtn.classList.add("collapsed");
                this._formArea.style.display = "none";
            } else {
                this._expandBtn.classList.add("expanded");
                this._expandBtn.classList.remove("collapsed");
                this._formArea.style.display = "initial";
            }
        }

        getSection() {
            return this._section;
        }

        getFormArea() {
            return this._formArea;
        }
    }

    export class PropertyPage extends Control {

        private _sectionProvider: IPropertySectionProvider;
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

        layout() {

        }

        private build() {

            const children = this.getElement().children;

            for (let i = 0; i < children.length; i += 1) {
                children.item(i).remove();
            }

            if (this._sectionProvider) {

                const list: PropertySection<any>[] = [];

                this._sectionProvider.addSections(this, list);

                for (const section of list) {
                    if (!this._sectionPaneMap.has(section.getId())) {
                        const pane = new PropertySectionPane(section);
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

                if (section.canEditNumber(n)) {
                    pane.createOrUpdateWithSelection();
                }
            }
        }

        getSelection() {
            return this._selection;
        }

        setSelection(sel: any[]): any {
            this._selection = sel;
            this.updateWithSelection();
        }

        setSectionProvider(provider: IPropertySectionProvider): void {
            this._sectionProvider = provider;

            this.build();
        }

        getSectionProvider() {
            return this._sectionProvider;
        }
    }

}