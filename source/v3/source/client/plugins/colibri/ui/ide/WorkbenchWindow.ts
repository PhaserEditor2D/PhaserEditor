/// <reference path="../controls/Control.ts" />

namespace colibri.ui.ide {

    export abstract class WorkbenchWindow extends controls.Control {

        private _toolbar: MainToolbar;
        private _clientArea: controls.Control;

        constructor() {
            super("div", "Window");

            this.setLayout(new controls.FillLayout(5));

            window.addEventListener("resize", e => {
                this.layout();
            });

            window.addEventListener(controls.EVENT_THEME_CHANGED, e => this.layout());

            this._toolbar = new MainToolbar();
            this._clientArea = new controls.Control("div", "WindowClientArea");
            this._clientArea.setLayout(new controls.FillLayout());

            this.add(this._toolbar);
            this.add(this._clientArea);

            this.setLayout(new WorkbenchWindowLayout());
        }

        getToolbar() {
            return this._toolbar;
        }

        getClientArea() {
            return this._clientArea;
        }

        getViews() {

            const views: ViewPart[] = [];

            this.findViews(this.getElement(), views);

            return views;
        }

        getView(viewId: string) {

            const views = this.getViews();

            return views.find(view => view.getId() === viewId);
        }

        private findViews(element: HTMLElement, views: ViewPart[]) {

            const control = controls.Control.getControlOf(element);

            if (control instanceof ViewPart) {

                views.push(control);

            } else {

                for (let i = 0; i < element.childElementCount; i++) {
                    const childElement = element.children.item(i);
                    this.findViews(<any>childElement, views);
                }
            }
        }

        protected createViewFolder(...parts: Part[]): ViewFolder {

            const folder = new ViewFolder();
            for (const part of parts) {
                folder.addPart(part);
            }

            return folder;
        }

        abstract getEditorArea(): EditorArea;
    }
}